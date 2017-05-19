import processing.core.PApplet;

public class Scope {

	int locX, locY, dimX, dimY;
	
	int color;
	
	PApplet parent;
	
	String name;
	
  // Helper function
	private int getY(int val) {
		return (int)(dimY - val / 65536.0f * (dimY - 1));
	}
	
  // Initialiser
	Scope(PApplet p, int locX, int locY, int dimX, int dimY, int color, String name) {
		this.parent = p;
		
		this.locX = locX;
		this.locY = locY;
		
		this.dimX = dimX;
		this.dimY = dimY;
		
		this.color = color;
		
		this.name = name;
	}
	
  
	public void draw(int[] data, int ptr) {
		
		parent.colorMode(PApplet.RGB);
		
    // Something is wrong, quit
		if (data.length < dimX) {
			PApplet.print("Insufficient buffer for desired scope dimension!\n");
			parent.exit();
		}
		
		parent.stroke(color);
		
    /* Rolling buffer drawing implementation */

    // Increment local index
		int k = ptr + 1;
		
    // If beyond limit, set to 0
		if (k >= data.length) k = 0;
		
    // Get the initial values
		int x1, x0 = 0;
		int y1, y0 = getY(data[k]);
		
		for (int i = 0; i < dimX; i++) {
			// Now, increment again, if beyond limit, set to 0
			if (++k >= data.length) k = 0;
			
      // Update coordinates
			x1 = x0 + 1;
			y1 = getY(data[k]);
			
      // Draw line
			parent.line(x0 + locX, y0 + locY, x1 + locX, y1 + locY);
			
      // Set 'old' as 'new' values
			x0 = x1;
			y0 = y1;
		}
		
		parent.fill(0, 0);
		parent.stroke(255, 255, 255, 255);
		
    // White border rectangle
		parent.rect(locX, locY, dimX, dimY);
		
    // Display 'name' of data represented
		parent.fill(255, 255, 255, 255);
		parent.text(name, locX + 10, locY + 15);
		
	}
	
}