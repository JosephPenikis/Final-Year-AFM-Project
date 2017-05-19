import processing.core.PApplet;
import processing.net.*;

public class Crosshair {
	int locX, locY, dimX, dimY;
	  
	int c;
	PApplet parent;

  // Show intensity values in each of the quadrants
  boolean intensityShow = false;
	  
  // Initialiser
	Crosshair(PApplet p, int locX, int locY, int dimX, int dimY, int c) {
	  this.parent = p;
		
	  this.locX = locX;
	  this.locY = locY;
	    
	  this.dimX = dimX;
	  this.dimY = dimY;
	    
	  this.c = c;
	}
	  
	void draw(float x, float y, float A, float B, float C, float D) {
  
    // Draw intensities in background if enabled
    if (intensityShow) { 
      parent.colorMode(PApplet.RGB, 1.0f);
      
      parent.fill(0.0f, 0.0f, A);
      parent.rect(locX, locY, dimX / 2, dimY / 2);
      parent.fill(0.0f, 0.0f, B);
      parent.rect(locX + dimX / 2, locY, dimX / 2, dimY / 2);
      parent.fill(0.0f, 0.0f, C);
      parent.rect(locX, locY + dimY / 2, dimX / 2, dimY / 2);
      parent.fill(0.0f, 0.0f, D);
      parent.rect(locX + dimX / 2, locY + dimY / 2, dimX / 2, dimY / 2);
    }
  
  
	  parent.colorMode(PApplet.ARGB, 0xFF);
		
    parent.fill(0, 0x00);
	  parent.stroke(255);
	  
    // Draw white border rectangle
	  parent.rect(locX, locY, dimX, dimY);
	    
    // Draw white center cross
	  parent.line(locX + dimX/2, locY, locX + dimX/2, locY + dimY);
	  parent.line(locX, locY + dimY/2, locX + dimX, locY + dimY/2);
	  
    // Draw text labels
	  parent.fill(0xff);
	  parent.text('A', locX + 0.05f * dimX, locY + 0.1f * dimY);
	  parent.text('B', locX + 0.55f * dimX, locY + 0.1f * dimY);
	  parent.text('C', locX + 0.05f * dimX, locY + 0.6f * dimY);
	  parent.text('D', locX + 0.55f * dimX, locY + 0.6f * dimY);
	  
	  parent.fill(c);
	  parent.stroke(c);
	  
    // Now draw the 'dot' position using the color set
	  parent.ellipse(locX + (x + 1.0f) * dimX * 0.5f, locY + (-y + 1.0f) * dimY * 0.5f, 5, 5);  
	}
}