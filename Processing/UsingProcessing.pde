import processing.core.PApplet;
import processing.net.*;
import processing.core.PFont;
import processing.core.PGraphics;
import controlP5.*;
import processing.serial.Serial;
import peasy.*;


// Objects used for GUI elements
ControlP5 cp5;
PFont font;

// Number of samples displayed on each Scope
int sampleLength = 256;

Serial port;

int val;
int[][] pdicValues;
float zoom;
int ptr = 0;

// Height data (256x256 fixed at moment)
float[][] contourMap = new float[256][256];
int currentXindex, currentYindex;

float maximum, minimum;

// Persistent objects for specific GUI elements
DropdownList serialPortList;
CheckBox checkBox;

// Custom GUI element objects
Scope[] scopes = new Scope[4];
//InputMapper map;
Crosshair cross;
DataViewer viewer;
ProfileView sliceView;
PiezoZ piezoZ;
AutoFocusController af;

// Object too which 2D contour map is drawn, increases performance
PGraphics viewerGraphic;

// GUI element sizes
final int BLOCK_DIM = 200;
final int SLIDER_DIM = 50;

// Laser command lines, length is longer than necessary, however, demonstrates long byte chains can be sent
static String key = "6JcP8EmMlV02NqtWbfeC9rQSkotTCtenOfo6E9UIYbBwiwVc7ZGtRpj1s0MWeGg\n";
static String laserOff = "PowerdownTheLaserImmediately\n";

public void settings() {
  // Set display size TODO: Make dynamic and based off proportions rather than absolute values
  size(1000, 670);
}

public void setup() {
  // Fill contour map with sine waves as default data (Mostly for test purposes)
  for (int i = 0; i < contourMap[0].length; i++) {
    for (int j = 0; j < contourMap.length; j++) {
      contourMap[j][i] = ((sin(40 * (float)i / 512 * 3.141592653589f) + 1.0f) + (sin(22 * (float)j / 512 * 3.141592653589f) + 1.0f)) / 4.0f;
    }
  }
  
  // Create object for GUI elements from ControlP5
  cp5 = new ControlP5(this);

  // Create memory to hold PDIC readings
  pdicValues = new int[4][sampleLength];
  
  // Create Crosshair used for displaying intensity and FES or Spot position
  cross = new Crosshair(this, 50, 260, BLOCK_DIM, BLOCK_DIM, color(255, 0, 0));

  // Create AF controller and Profile Viewer
  af = new AutoFocusController(this, cp5, 680, 50, 300, 410);
  sliceView = new ProfileView(this, cp5, 270, 480, 390, 100, color(255, 0, 0));

  // Create DataViewer (for contour map) and generate initial image
  viewer = new DataViewer(this, 270, 50, 390, 410);    
  viewer.updateGraphic(contourMap, 1.0f, 0.0f);

  piezoZ = new PiezoZ(this, cp5, 680, 480, 300, 100);

  // As little sense as this button makes sense being declared here, it is currently necessary as the callback is handled within the main class
  // TODO: Move this inside of the appropriate GUI element
  cp5.addButton("Start").setPosition(390, 430).setSize(150, 20);

  // Collect and display a list of port names in a dropdown box for the user to select
  String[] portNames = Serial.list();

  serialPortList = cp5.addDropdownList("serialPorts").setPosition(50, 20).setWidth(200);
  for (int i = 0; i < portNames.length; i++) serialPortList.addItem(portNames[i], i);

  // Switch color mode to utilise Hues and set 4 discrete values. Allows each scope to have a unique color programatticly
  colorMode(HSB, scopes.length);

  // Now create all the scopes in their appropriate positions
  for (int i = 0; i < 2; i++) {
    for (int j = 0; j < 2; j++) {
      scopes[i * 2 + j] = new Scope(this, (j * (int)(0.95f * BLOCK_DIM / 2 + 10)) + 50, (i * (int)(0.95f * BLOCK_DIM / 2 + 10)) + 50, (int)(0.95f * BLOCK_DIM / 2), (int)(0.95f * BLOCK_DIM / 2), color(i * 2 + j, scopes.length, scopes.length), "Channel: " + (i * 2 + j));
    }
  }
  
  // Add all the ncessary CP5 elements
  cp5.addSlider("Scale:").setPosition(50 + 0.02f * BLOCK_DIM, 480).setSize((int)(0.96f * BLOCK_DIM), 20).setRange(0.0f, 10.0f).setValue(1.0f).setScrollSensitivity(0.01f).getCaptionLabel().align(ControlP5.CENTER, ControlP5.TOP_OUTSIDE);

  // Passkey dials
  for (int i = 0; i < 6; i++) {
    cp5.addKnob("Key " + i).setPosition(320 + i * 50, 600).snapToTickMarks(true).setRange(0, 9).setNumberOfTickMarks(9).setSize(25, 25);
  }

  cp5.addButton("Laser").setPosition(360, 640).setSize(200, 20);

  cp5.addButton("Export").setPosition(360, 20).setSize(90, 20);
  cp5.addButton("Show3D").setPosition(460, 20).setSize(90, 20);

  checkBox = cp5.addCheckBox("FES").setPosition(50 + 0.1f * BLOCK_DIM, 510).setSize(20, 20).addItem("Show FES", 0).addItem("Show Intensity", 1);

  // Ensure higher framerate. Not really ideal right now, but speeds up process as commands are (horribly) frame rate driven
  frameRate(60);
}

// Control event handling GUI element callbacks from ControlP5
public void controlEvent(ControlEvent theEvent) {

  // First couple of elements are dummy catches, program throws errors if this code is excluded although
  // it serves no other purpose here
  if (af != null && af.controlEvent(theEvent)) {
    return;
  } else if (theEvent.isFrom(checkBox)) {
    println("Checkbox toggle...");
  } else if (theEvent.getController().getName() == "serialPorts") {

    // Serial port selected in dropdown
    
    // Kill any old connections
    if (port != null) {
      port.stop();
      port = null;
    }

    // Get the serial index and do an out of bounds check to prevent crashing when referencing non-existent port
    int portIndex = (int)theEvent.getController().getValue();

    if (portIndex < 0 || portIndex > Serial.list().length - 1) return;

    // Try and make a connection to the selected port
    try {
      port = new Serial(this, Serial.list()[portIndex], 115200);
    } 
    catch (Exception e) {
      System.err.println("Error opening serial port!");
      e.printStackTrace();
    }

    // Flush the port and beginning to remove Junk and set 0xfe as the byte to perform callback on.
    // Note: 0xfe will occur outwith 'end' byte as well, however, the serialEvent handles these conditions by
    //  checking byte length
    port.clear();
    port.bufferUntil(0xfe);
  }
}

// Used as a background storage variables for the voice coil control values
int dacVals[] = new int[3];

// Now, handle any incoming serial data
public void serialEvent(Serial p) {
  try {
    // If we don't have a full packet, return
    if (p.available() < 10) {
      //p.clear();
      return;
    }

    int val = p.read();

    // Receiving data from the PDIC ADC
    if (val == 0xff) {

      for (int i = 0; i < 4; i++) {
        pdicValues[i][ptr] = (p.read() << 8) | (p.read());
      }
    
      // Reset the pointer if over end (ring buffer configuration)
      if (++ptr >= sampleLength) {
        ptr = 0;
      }
    // Reading a returned Z value from a scan
    } else if (val == 0xef) {
      val = (p.read() << 8) | (p.read());

      // Dummy reads, TODO: Replace with something less wasteful
      // Currently using a fixed packet size for all data communications to simplify interface
      for (int i = 0; i < 6; i++) {
        p.read();
      }

      println("Data received...");

      // Supposedly a frame containing the z value from the selected location
      contourMap[currentXindex][currentYindex] = (float)val / (float)65535;

      // Get max and minimums
      maximum = max(maximum, contourMap[currentXindex][currentYindex]);
      minimum = min(minimum, contourMap[currentXindex][currentYindex]);

      // Increment x index and if over edge, reset it and increment Y
      if (++currentXindex >= contourMap[0].length) {
        currentYindex++;
        currentXindex = 0;

        if (currentYindex > contourMap.length) {
          // TODO: Implement appropriate action once scan is completed...
          // Not handled yet as not tested this far
          println("Scan complete... event not yet handled!");
        } else {
          // Move the "slice" control so displayed slice is the one currently being sampled
          cp5.getController("slice").setValue(currentYindex);
        }
      }

      // Redraw image
      viewer.updateGraphic(contourMap, maximum, minimum);

      // Send next xy-location
      port.write(0xff);
      port.write(0xcf);
      // Data payload
      port.write(currentXindex & 0xff);
      port.write((currentXindex >> 8) & 0xff);
      port.write(currentYindex & 0xff);
      port.write((currentYindex >> 8) & 0xff);

      // Dummy writes
      port.write(0xff);
      port.write(0xff);
    }

    // If the autofocus settings has been changed, push the updates to the arduino
    if (af.invalid) {

      println("Updating DAC...");

      p.write(0xff);
      p.write(0xdf);

      af.updateChannels(dacVals, 4095);

      for (int i = 0; i < dacVals.length; i++) {
        p.write(dacVals[i] & 0xff);
        p.write((dacVals[i] >> 8) & 0xff);
      }

      // Update complete
      af.invalid = false;
    }
    
    // Similarly, if the Z-Piezo setting has been adjusted, push update 
    if (piezoZ.dataInvalid) {
      println("Updating Piezo Z-axis..."); 

      port.write(0xff);
      port.write(0xcf);

      // Dummy write to X/Y (Arduino interprets as Z update rather than an XY update during a scan)
      p.write(0xff);
      p.write(0xff);
      p.write(0xff);
      p.write(0xff);

      p.write(piezoZ.pos & 0xff);
      p.write((piezoZ.pos >> 8) & 0xff);

      // Update cleared
      piezoZ.dataInvalid = false;
    }

    // FLush the buffer again to keep time (Otherwise the buffer takes too long to process), loss of data, but not an issue when in streaming mode
    p.clear();
  } 
  catch(Exception e) {
    println("Error parsing: ");
    e.printStackTrace();
  }
}

int xptr = 0, yptr = 0;
public float FES;

public void draw() {
  // Clear the draw buffer and set background to black
  clear();
  background(0);

  // Draw the scopes with their appropriate values
  for (int s = 0; s < scopes.length; s++) {
    scopes[s].draw(pdicValues[(scopes.length - 1) - s], ptr);
  }

  // Draw the other GUI elements
  af.draw();
  sliceView.draw(contourMap);
  viewer.draw(sliceView.getSliceX());
  piezoZ.draw();

  
  float scale = cp5.getController("Scale:").getValue();

  // Compute the FES
  float A = (float)pdicValues[3][ptr];
  float B = (float)pdicValues[2][ptr];
  float C = (float)pdicValues[1][ptr];
  float D = (float)pdicValues[0][ptr];

  float sum = A + B + C + D;

  // Find the effective weighted location
  float dX = (A + C) - (B + D);  
  float dY = (A + B) - (C + D);

  FES = ((A - B) + (C - D));
  
  // Push new value to the focus controller 
  af.updateFES(FES / sum);

  cross.intensityShow = checkBox.getState(1);

   // Draw the dot where required depending on mode
  if (checkBox.getState(0))
    cross.draw(scale * FES / sum, 0.0f, A / sum, B / sum, C / sum, D / sum);
  else
    cross.draw(scale * dX / sum, scale * dY / sum, A / sum, B / sum, C / sum, D / sum);
}

// Dummy password
int password = 123456;

// Handler for the "Laser" Button
public void Laser(int theValue) {

  int pass = 0, mult = 1;
  
  // Read in the passkey as a value
  for (int i = 5; i >= 0; i--) {
    pass += cp5.getController("Key " + i).getValue() * mult;
    mult *= 10;
  }

  // If password correct, send the command 'key' to enable the laser
  if (pass == password) {
    println("Password correct!!!");  

    for (int i = 5; i >= 0; i--) {
      cp5.getController("Key " + i).setValue(0.0f);
    }

    if (port != null) {
      port.write(0xff);
      port.write(0xef);
      port.write(key);
    }

    println("Length: " + key.length());
  } else {
    // Otherwise, send the command to power down the laser (Anything over than the correct password causes this action)
    
    println("Wrong Password! Powering Down Laser");  

    if (port != null) {
      port.write(0xff);
      port.write(0xef); 
      port.write(laserOff);
    }
  }
}

// Button handler for "Scan", starts the scan process off
public void Scan(int theValue) {
  PApplet.println("Attempting autofocus!");
  // Get initial scan location
  af.getNextScanLocation(FES);
}

// Button handler for the focus controllers "Plus" button
public void Plus(int theValue) {
  if (af.stepSize <= 64)
    af.stepSize *= 2;
}

// Button handler for the focus controllers "Minus" button
public void Minus(int theValue) {
  if (af.stepSize > 1)
    af.stepSize /= 2;
}

// Button handler for the Z-Piezo "Plus" button
public void ZPlus(int theValue) {
  piezoZ.pos += cp5.getController("Z Step Size (lsb)").getValue();

  // Cap the value
  if (piezoZ.pos > 65535) piezoZ.pos = 65535;

  piezoZ.dataInvalid = true;
}

// Button handler for the Z-Piezo "Minus" button
public void ZMinus(int theValue) {
  piezoZ.pos -= cp5.getController("Z Step Size (lsb)").getValue();

  // Cap the value
  if (piezoZ.pos < 0) piezoZ.pos = 0;

  piezoZ.dataInvalid = true;
}

public void Start(int theValue) {

  if (af.afScanStarted) {
    println("Focus scan in operation, cannot complete task...");
    return;
  }		

  println("Starting AFM scan...");

  // Clear the current data set
  for (int i = 0; i < contourMap[0].length; i++) {
    for (int j = 0; j < contourMap.length; j++) {
      contourMap[j][i] = 0.0f;
    }
  }

  // Set default values
  currentXindex = 0;
  currentYindex = 0;

  maximum = 0.0f;
  minimum = 1.0f;

  // Redraw view
  viewer.updateGraphic(contourMap, 1.0f, 0.0f);

  // Send initial xy-location
  port.write(0xff);
  port.write(0xcf);
  // Data payload
  port.write(0x00);
  port.write(0x00);
  port.write(0x00);
  port.write(0x00);

  // Dummy writes
  port.write(0xff);
  port.write(0xff);

  // Handled here on in by the arduino and serial event
}

// Button handler for the export key
public void Export(int theValue) {
  saveBMP();
}

public void Show3D(int theValue) {
  Export(0);

  // Open new applet, this applet loads the previosly exported image in order to render a 3D view
  // Sends argument with sketch path and image name to new applet
  String[] args1 = {"3D View", sketchPath("") + "output.bmp"};
  Viewer3D v3 = new Viewer3D();  
  PApplet.runSketch(args1, v3);
}

public void saveBMP() {
  PGraphics g = createGraphics(contourMap[0].length, contourMap.length);

  g.beginDraw();

  g.clear();

  //p.smooth();

  g.stroke(0x00, 0x00, 0x00, 0x00);
  g.fill(0xff, 0x00, 0x00, 0xff);
  g.strokeWeight(0.0f);

  // Generate the bitmap and save it
  for (int i = 0; i < contourMap[0].length - 1; i++) {
    g.beginShape(PApplet.QUAD_STRIP);
    for (int j = 0; j < contourMap.length; j++) {				
      g.fill((int)PApplet.floor(contourMap[j][i] * 255.0f));

      g.rect(i, j, 1.0f, 1.0f);
    }
    g.endShape();
  }

  g.endDraw();

  g.save("output.bmp");
}