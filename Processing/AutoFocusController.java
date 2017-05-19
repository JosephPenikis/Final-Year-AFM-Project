import controlP5.CheckBox;
import controlP5.ControlEvent;
import controlP5.ControlP5;
import controlP5.Textarea;
import processing.core.PApplet;

/*

  NOTE: Current implementation is NOT an autofocus controller as the class name suggests. Currently only supports a manual mode.

*/

public class AutoFocusController {

	int locX, locY, dimX, dimY;
	
	private float restrictedX = 0.5f, restrictedY = 0.5f;
	private float originalRestrictedX, originalRestrictedY;
	
	PApplet parent;
	
	public float widgetX = 0.5f, widgetY = 0.5f, tilt = 0.0f;
	
	@Deprecated
	private float centerX = 0.5f, centerZp = 0.5f, centerZn = 0.5f;
	
	private boolean caution = false;
	
	ControlP5 p5;
	CheckBox afMode;
	
  // Stores the generated intensity map, increasing resolution has a significant performance impact right now
	private float[][] intensityMap = new float[64][64];
	
	public AutoFocusController(PApplet p, ControlP5 p5, int locX, int locY, int dimX, int dimY) {
		this.parent = p;
		this.p5 = p5;
		
		this.locX = locX;
		this.locY = locY;
		
		this.dimX = dimX;
		this.dimY = dimY;
		
    // Add all the ControlP5 controls to the GUI
		p5.addSlider("Restrict X").setPosition(locX + 0.25f * dimX, locY + 0.48f * dimY).setSize((int)(0.2f * dimX), 15).setRange(0.0f, 1.0f).setValue(0.5f).setScrollSensitivity(0.001f).getCaptionLabel().align(ControlP5.CENTER, ControlP5.TOP_OUTSIDE);
		p5.addSlider("Restrict Y").setPosition(locX + 0.55f * dimX, locY + 0.48f * dimY).setSize((int)(0.2f * dimX), 15).setRange(0.0f, 1.0f).setValue(0.5f).setScrollSensitivity(0.001f).getCaptionLabel().align(ControlP5.CENTER, ControlP5.TOP_OUTSIDE);
		
		p5.addSlider("Step Size (lsb)").setPosition(locX + 0.33f * dimX, locY + 0.4f * dimY).setSize((int)(0.33f * dimX), 15).setRange(1, 128).setValue(64).setDecimalPrecision(0).getCaptionLabel().align(ControlP5.CENTER, ControlP5.TOP_OUTSIDE);
		
		afMode = p5.addCheckBox("AFMode").setPosition(locX + 0.2f * dimX, locY + 0.3125f * dimY).setSize(10, 10).addItem("Scan Away?", 0);
		p5.addButton("Scan").setPosition(locX + 0.5f * dimX, locY + 0.3f * dimY).setSize(100, 20);
		
		p5.addButton("Plus").setPosition(locX + 0.4f * dimX, locY + 0.07f * dimY).setSize(25, 20);
		p5.addButton("Minus").setPosition(locX + 0.4f * dimX, locY + 0.18f * dimY).setSize(25, 20);
		
		p5.addSlider("Contrast").setPosition(locX + 0.05f * dimX, locY + 0.05f * dimY).setSize((int)(0.33f * dimX), 15).setRange(0.0f, 8.0f).setValue(1.0f).getCaptionLabel().align(ControlP5.CENTER, ControlP5.TOP_OUTSIDE);
		
		parent.colorMode(PApplet.RGB, 1.0f);	
	}
	
  // Catch the control event that occurs when the checkbox is toggled
	public boolean controlEvent(ControlEvent theEvent) {
		if (theEvent.isFrom(afMode)) {
			PApplet.println("Checkbox toggled!");
			return true;
		}
		return false;
	}
	
	public boolean invalid = true;
	
	private float oldWidgetX, oldWidgetY;
	
  // Stores the state of the buttons (edge-detection)
	private boolean[] bMap = new boolean[5];
	
  // Determines the step size of lsb and the default step size
	private float lsbStep = 1.0f / (float)4095;
	public int stepSize = 64;
	
	private int samples;
	private float sampleSum;
	
	void draw() {
		
		// If autofocus running and data is valid, move to next location
		if (afScanStarted && !invalid)
			getNextScanLocation(0.0f);
				
		// Update the restriction settings
		restrictedX = p5.getController("Restrict X").getValue();
		restrictedY = p5.getController("Restrict Y").getValue();
		
    // Get the current mouse position relative to the bounds of the GUI element
		int mouseX = parent.mouseX - locX;
		int mouseY = parent.mouseY - locY;
		
		// Disable mouse control when scanning as well
		if (!afScanStarted && parent.mousePressed == true && !(mouseX < 0 || mouseX > dimX || mouseY < 0 || mouseY > dimY)) {
			float relX = (mouseX - 0.1f * dimX) / (0.8f * dimX);
			float relY = (mouseY - 0.55f * dimY) / (0.4f * dimY);
			
			float dist = PApplet.sqrt(PApplet.sq(widgetX - relX) + PApplet.sq(widgetY - relY));
									
			if (dist < 0.1f && relX >= 0.0f && relX <= 1.0f && relY >= 0.0f && relY <= 1.0f) {
				widgetX = relX;
				widgetY = relY;
			}
			
			// Now hitbox the buttons
			// Center
			if (!bMap[0] && mouseX > 0.63f * dimX && mouseX < 0.69f * dimX && mouseY > 0.12f * dimY && mouseY < (0.12f * dimY + 0.06 * dimX)) {
				bMap[0] = true;
				widgetX = 0.5f;
				widgetY = 0.5f;
			} else if (!bMap[1] && mouseX > 0.54f * dimX && mouseX < 0.6f * dimX && mouseY > 0.12f * dimY && mouseY < (0.12f * dimY + 0.06f * dimX)) {
				bMap[1] = true;
				widgetX -= lsbStep * stepSize;
			} else if (!bMap[2] && mouseX > 0.72f * dimX && mouseX < 0.78f * dimX && mouseY > 0.12f * dimY && mouseY < (0.12f * dimY + 0.06f * dimX)) {
				bMap[2] = true;
				widgetX += lsbStep * stepSize;
			} else if (!bMap[3] && mouseX > 0.63f * dimX && mouseX < 0.69f * dimX && mouseY > (0.12f * dimY - 0.09f * dimX) && mouseY < (0.12f * dimY - 0.03f * dimX)) {
				bMap[3] = true;
				widgetY -= lsbStep * stepSize;
			} else if (!bMap[4] && mouseX > 0.63f * dimX && mouseX < 0.69f * dimX && mouseY > (0.12f * dimY + 0.09f * dimX) && mouseY < (0.12f * dimY + 0.15f * dimX)) {
				bMap[4] = true;
				widgetY += lsbStep * stepSize;
			}
			
      // Constrain the values so they are within range
			widgetX = PApplet.constrain(widgetX, 0.0f, 1.0f);
			widgetY = PApplet.constrain(widgetY, 0.0f, 1.0f);
			
      // If the widget is outside of the bounds, set the caution flag
			if (widgetX - ((1.0f - restrictedX) / 2.0f) < 0.0f || widgetX + ((1.0f - restrictedX) / 2.0f) > 1.0f || widgetY - ((1.0f - restrictedY) / 2.0f) < 0.0f || widgetY + ((1.0f - restrictedY) / 2.0f) > 1.0f) {
				caution = true;
			} else
				caution = false;
			
      // If the widget position has changed, mark data as invalid so parent can send it when ready
			if (oldWidgetX != widgetX || oldWidgetY != widgetY) 
				invalid = true; // Mark data as invalid so main program can update as appropriate
			
			// Now store the new values as the old for the next iteration
			oldWidgetX = widgetX;
			oldWidgetY = widgetY;	
		} else if (!parent.mousePressed) {
      // If mouse isn't pressed, clear all the button states
			for (int i = 0; i < bMap.length; i++) {
				bMap[i] = false;
			}
		}
		
		
		parent.colorMode(PApplet.NORMAL);
		
		parent.pushMatrix();
		
		parent.translate(locX, locY);
		

		// Draw arrow controls
		parent.stroke(0xff, 0x00, 0x00, 0xff);

    // Draw text giving ray value (0.0f-->1.0f)
		parent.text(Integer.toString(stepSize), 0.42f * dimX, 0.16f * dimY);
		parent.text("X: " + widgetX, 0.8f * dimX, 0.1f * dimY);
		parent.text("Y: " + widgetY, 0.8f * dimX, 0.23f * dimY);
		
		parent.stroke(0x00, 0x00, 0x99, 0xff);
				
		// Center
		if (bMap[0]) 
			parent.fill(0x44, 0x44, 0xbb, 0xff);
		else
			parent.fill(0x00, 0x00, 0xbb, 0xff);
		
		parent.rect(0.63f * dimX, 0.12f * dimY, 0.06f * dimX, 0.06f * dimX);
		
		// Left (-x)
		if (bMap[1]) 
			parent.fill(0x44, 0x44, 0xbb, 0xff);
		else
			parent.fill(0x00, 0x00, 0xbb, 0xff);
		
		parent.triangle(0.54f * dimX, 0.12f * dimY + 0.03f * dimX,	
						0.60f * dimX, 0.12f * dimY,
						0.60f * dimX, 0.12f * dimY + 0.06f * dimX);
		// Right (+x)
		if (bMap[2]) 
			parent.fill(0x44, 0x44, 0xbb, 0xff);
		else
			parent.fill(0x00, 0x00, 0xbb, 0xff);
		
		parent.triangle(0.78f * dimX, 0.12f * dimY + 0.03f * dimX,	
						0.72f * dimX, 0.12f * dimY,
						0.72f * dimX, 0.12f * dimY + 0.06f * dimX);
		
		// Up (-z)
		if (bMap[3]) 
			parent.fill(0x44, 0x44, 0xbb, 0xff);
		else
			parent.fill(0x00, 0x00, 0xbb, 0xff);
		
		parent.triangle(0.66f * dimX, 0.12f * dimY - 0.09f * dimX,
						0.63f * dimX, 0.12f * dimY - 0.03f * dimX,
						0.69f * dimX, 0.12f * dimY - 0.03f * dimX);
		
		// Down (+z)
		if (bMap[4]) 
			parent.fill(0x44, 0x44, 0xbb, 0xff);
		else
			parent.fill(0x00, 0x00, 0xbb, 0xff);
		
		parent.triangle(0.66f * dimX, 0.12f * dimY + 0.15f * dimX,
						0.63f * dimX, 0.12f * dimY + 0.09f * dimX,
						0.69f * dimX, 0.12f * dimY + 0.09f * dimX);
		
		parent.fill(0x00, 0x00);
		parent.stroke(0xff, 0xff);
		
    // Draw border
		parent.rect(0, 0, dimX, dimY);
		
		// Determine the starting position by calculating the upper left coordinates of the restricted square
		float xStart = (0.1f + 0.8f * (1.0f - originalRestrictedX) / 2.0f) * dimX;
		float yStart = (0.55f + 0.4f * (1.0f - originalRestrictedY) / 2.0f) * dimY;
		
    // Re-calculate the average on each iteration
		float localAverage = sampleSum / (float)samples;
	  float mult = p5.getController("Contrast").getValue();
	    
    // Now draw the intensity map, TODO: Make this better performing
		for (int i = 0; i < intensityMap[0].length; i++) {
			for (int j = 0; j < intensityMap.length; j++) {
        // Map the value to a color intensity
				float c = mult * 255.0f * intensityMap[j][i] / localAverage;    
		        
		    if (c >= 255.0f) c = 254.0f;
				
				parent.stroke(c, 0x00, 0x00);
        // Draw the point
				parent.point(xStart + (0.8f * originalRestrictedX * dimX) * ((float)i / (float)intensityMap[0].length), yStart + (0.4f * originalRestrictedY * dimY) * ((float)j / (float)intensityMap.length));

        // NOTE: Floating point rounding errors currently introduce banding in the rendered result, look into fixing
			}
		}
		
		parent.fill(0x00, 0x00);
		parent.stroke(0xff, 0x00, 0x00, 0xff);
  		
    // Draw red limit rectangles
		parent.rect(0.1f * dimX , 0.55f * dimY, 0.8f * dimX, 0.4f * dimY);
		
		// Draw yellow restricted rectangle if not same size as red
		if (restrictedX != 1.0f || restrictedY != 1.0f) {
			parent.stroke(0xff, 0xff, 0x00, 0xff);
			
			parent.rect((0.1f + 0.8f * (0.5f - restrictedX / 2.0f)) * dimX, (0.55f + 0.4f * (0.5f - restrictedY / 2.0f)) * dimY, 0.8f * restrictedX * dimX, 0.4f * restrictedY * dimY);
		}
		
		parent.stroke(0x00, 0xff, 0xff);		
		parent.strokeWeight(2.0f);
		
    // Calculate the coordinate offsets depending on the tilt of the head (currently unused)
		float delX = 0.05f * PApplet.cos(PApplet.radians(tilt * 20.0f));
		float delY = 0.05f * PApplet.sin(PApplet.radians(tilt * 20.0f));
		// Draw widget
		parent.line((0.15f + widgetX * 0.7f - delX) * dimX, (0.57f + widgetY * 0.36f - delY) * dimY,
					(0.15f + widgetX * 0.7f + delX) * dimX, (0.57f + widgetY * 0.36f + delY) * dimY);
		
		
		parent.stroke(0xff, 0xff, 0x00);
		parent.fill(0xff, 0xff, 0x00);
		
    // Draw caution symbol if flag enabled
		if (caution) {
			parent.triangle(0.85f * dimX, 0.57f * dimY, 0.82f * dimX, 0.6f * dimY, 0.88f * dimX, 0.6f * dimY);
			
			parent.fill(0x00);
			parent.text('!', 0.845f * dimX, 0.6f * dimY);
		}
		
		parent.popMatrix();
	}
	
  // Get the raw data to be sent to the DAC from the current settings
	public void updateChannels(int[] vals, int dacRange) {
		if (vals.length != 3) {
			PApplet.println("!!! Not enough channels passed into updateChannels !!!");
			return;
		}
		
		vals[0] = PApplet.floor(widgetX * dacRange);
		vals[1] = PApplet.floor(widgetY * dacRange);
		vals[2] = PApplet.floor((1.0f - widgetY) * dacRange);
	}
	
	public boolean afScanStarted = false;
	// Flag to indicate direction of movement
	private boolean scanForward = true;
	
	private float FES;
	
  // Sets local copy of the FES
	public void updateFES(float FES) {
		this.FES = FES;
	}

	private float maxDeltaFES;
	
  // Called on both scan initialization and during runtime
	public void getNextScanLocation(float initialFES) {		
		// Haven't started, set values to starting position
		if (!afScanStarted) {
			
			this.FES = initialFES;
			maxDeltaFES = 0.0f;
			
			widgetX = (1.0f - restrictedX) / 2.0f;
			
			widgetY = (1.0f + restrictedY * (afMode.getState(0) ? 1.0f : -1.0f)) / 2.0f;
			
			// Scan started, so lock out control
			afScanStarted = true;

			// Invalidate the data and force update
			invalid = true;
			
			samples = 0;
			
			// Reset intensity map
		    for (int i = 0; i < intensityMap[0].length; i++) {
		      for (int j = 0; j < intensityMap.length; j++) {
		        intensityMap[j][i] = 0.0f;
		      }
		    }
			
			// Save these for rendering purposes
			originalRestrictedX = restrictedX;
			originalRestrictedY = restrictedY;
			
			return;
		}
		
		// First, check FES from previous movement to see if any change has occurred
		float deltaFES = PApplet.sqrt(PApplet.sq(initialFES - FES));
		
		if (deltaFES > maxDeltaFES) maxDeltaFES = deltaFES;
				
    // Now find the corrected coordinate matching this on the intensity map
		float xEdge = (1.0f - originalRestrictedX) / 2.0f;
		float yEdge = (1.0f - originalRestrictedY) / 2.0f;
		
		int xIndex = PApplet.floor(((widgetX - xEdge) / originalRestrictedX) * (float)(intensityMap.length - 1));
		int yIndex = PApplet.floor(((widgetY - yEdge) / originalRestrictedY) * (float)(intensityMap[0].length - 1));
		

		PApplet.println(xIndex + ",   " + yIndex);
		
	  // Constrain to prevent out of range errors
		yIndex = PApplet.constrain(yIndex, 0, intensityMap[0].length - 1);
		xIndex = PApplet.constrain(xIndex, 0, intensityMap.length - 1);
		
		// Writing to a new location, so increase sample count
		if (intensityMap[yIndex][xIndex] == 0.0f)
			samples++;
		
		// We don't have a bin for each of the locations (too performance intensive in rendering), so show the maximum observed in the respective bin
	    // Ignore 0,0 for now as a bug fix
		if (xIndex != 0 && yIndex != 0 && deltaFES > intensityMap[yIndex][xIndex]) {
			sampleSum -= intensityMap[yIndex][xIndex];	// Remove the original value from the summation and add the new one
			sampleSum += deltaFES;
			intensityMap[yIndex][xIndex] = deltaFES;
		}
	
		float stepSize = p5.getController("Step Size (lsb)").getValue() / 4095;
		
    // Increment the widget in the appropriate direct
		widgetY += (stepSize * (scanForward ? 1.0f : -1.0f));
		
		// If out of bounds, reverse direction and move to new z
		if (widgetY > (1.0f + originalRestrictedY) / 2.0f || widgetY < (1.0f - originalRestrictedY) / 2.0f) {
			scanForward = !scanForward;
			widgetX += stepSize * (afMode.getState(0) ? -1.0f : 1.0f);
		}
		
		// Scan finished
		if (widgetX > (1.0f + originalRestrictedX) / 2.0f || widgetX < (1.0f - originalRestrictedX) / 2.0f) {
			afScanStarted = false;
			
			PApplet.println("Max Delta FES: " + maxDeltaFES + ", initial FES: " + initialFES);
			
			return;
		}

		// Invalidate the data and force update
		invalid = true;
	}
}