package ReplGraphics.XYRPlot;
/*
class steriodPanel extends JPanel{
	private double x, y;
	
	public steriodPanel(){
		//point pos = DistanceSensorMath.distanceToPosition(5, 5, 0);
		//System.out.println("set " + pos.hori + " " + pos.vert);
		//setPos(pos.hori, pos.vert);
	}
	
	@Override
	protected void paintComponent(Graphics gr) {
		super.paintComponent(gr);
		
		var g = new scaleGraphics(gr, 8.0, 12.0, getWidth(), getHeight());
		//5, 3
		
		System.out.println(
				"\nup " + slider1.getValue()/10.0 +
						"\ndown " + slider2.getValue()/10.0 +
						"\nleft " + slider3.getValue()/10.0 +
						"\nright " + slider4.getValue()/10.0);
		
		DistanceSensorMath.distanceToPosition(
				slider3.getValue()/10.0,
				slider4.getValue()/10.0,
				slider1.getValue()/10.0,
				slider2.getValue()/10.0, g);
		
		// draw robot
		g.setColor(Color.orange);
		g.fillOval(x, y, 0.1, 0.1);
		
		// print coords
		g.setColor(Color.black);
		g.setFont(new Font("Trebuchet MS", Font.PLAIN, 14));
		g.drawString(String.format("(x: %.2f y: %.2f)", x, y), 10, 20);
		
		// grid
		g.setColor(Color.gray);
		for(int x = 0; x < 8; x += 1){
			g.drawLine(x, 0, x, 12);
		}
		for(int y = 0; y < 12; y += 1){
			g.drawLine(0, y, 8, y);
		}
		
	}
	
	public void setPos(double hori, double vert){
		x = hori;
		y = vert;
		
		repaint();
	}
}

 */