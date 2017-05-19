import controlP5.ControlP5;
import processing.core.PApplet;

public class ProfileView {
  int locX, locY, dimX, dimY;

  int color;

  PApplet parent;
  ControlP5 p5;

  public int sliceX, sliceY;

  ProfileView(PApplet p, ControlP5 p5, int locX, int locY, int dimX, int dimY, int color) {
    this.parent = p;
    this.p5 = p5;

    this.locX = locX;
    this.locY = locY;

    this.dimX = dimX;
    this.dimY = dimY;

    this.color = color;

    p5.addSlider("slice").setPosition(locX + 0.3f * dimX, locY + 0.9f * dimY).setSize((int)(0.4f * dimX), (int)(0.05f * dimY)).setRange(0.0f, 255).setValue(0).setDecimalPrecision(0).setScrollSensitivity(1.0f / 32.0f);
  }

  public void draw(float data[][]) {

    parent.pushMatrix();

    parent.colorMode(PApplet.RGB);

    parent.translate(locX, locY);

    parent.fill(0x00, 0x00);
    parent.stroke(0xff, 0xff);

    // Draw white border
    parent.rect(0, 0, dimX, dimY);

    // Draw the graphs axes
    parent.line(0.15f * dimX, 0.1f * dimY, 0.15f * dimX, 0.81f * dimY);
    parent.line(0.14f * dimX, 0.8f * dimY, 0.8f * dimX, 0.8f * dimY);

    // Calculate the stepping and scale values for the data points
    float step = (0.65f * dimX) / data.length;
    float yScale = 0.7f * dimY;

    sliceX = (int)p5.getController("slice").getValue();

    parent.stroke(color, 0xff);

    // Now draw line plot
    for (int i = 0; i < data[0].length - 1; i++) {
      parent.line(0.15f * dimX + i * step, 0.8f * dimY - yScale * data[i][sliceX], 0.15f * dimX + (i + 1) * step, 0.8f * dimY - yScale * data[i+1][sliceX]);
    }

    parent.popMatrix();
  }

  // Helper function to get the current 'slice' index from the slider
  public int getSliceX() {		
    return(int)p5.getController("slice").getValue();
  }


  public void forcePosition(int pos) {
    p5.getController("slice").setValue(pos);
  }
}