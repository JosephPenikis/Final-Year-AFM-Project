import controlP5.ControlP5;
import processing.core.PApplet;

public class PiezoZ {
	int locX, locY, dimX, dimY;

	PApplet parent;
	ControlP5 p5;
	
	int pos;
	
	boolean dataInvalid = false;
	
	PiezoZ(PApplet p, ControlP5 p5, int locX, int locY, int dimX, int dimY) {
		this.parent = p;
		this.p5 = p5;
		
		this.locX = locX;
		this.locY = locY;
		
		this.dimX = dimX;
		this.dimY = dimY;
		
		p5.addButton("ZPlus").setPosition(locX + 0.3f * dimX, locY + 0.32f * dimY).setSize(35, 20);
		p5.addButton("ZMinus").setPosition(locX + 0.3f * dimX, locY + 0.5f * dimY).setSize(35, 20);
		
		p5.addSlider("Z Step Size (lsb)").setPosition(locX + 0.46f * dimX, locY + 0.42f * dimY).setSize((int)(0.33f * dimX), 15).setRange(1, 1024).setValue(512).setDecimalPrecision(0).getCaptionLabel().align(ControlP5.CENTER, ControlP5.TOP_OUTSIDE);
	}
	
	public void draw() {
		parent.pushMatrix();
		parent.translate(locX, locY);
		
		parent.fill(0x00, 0x00, 0x00, 0x00);
		parent.stroke(0xff, 0xff, 0xff, 0xff);
		
    // Border rectangle
		parent.rect(0, 0, dimX, dimY);
		
		parent.fill(0xff, 0xff, 0xff, 0xff);

    // Display current value
		parent.text((pos / 65535.0f), 0.15f * dimX, 0.54f * dimY);
		
		parent.popMatrix();
	}
}