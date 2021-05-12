package ReplGraphics.XYRPlot;

import ReplGraphics.DataHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;

public class XYRPlot implements DataHandler {
	steriodPanel p;

	@Override
	public void accept(String[] params) {
//		for(String thing : params){
//			System.out.println("pram: " + thing);
//		}
		try {
			p.x = Double.valueOf(params[1]);
			p.y = Double.valueOf(params[2]);
			p.r = Double.valueOf(params[3]);
		}catch (NumberFormatException e){
			System.out.println("param invalid");
		}
		p.repaint();
	}

	public XYRPlot(){
		p = new steriodPanel();
		JFrame f = new JFrame("loc");
		p.setSize(new Dimension(scaleGraphics.margin + 800 / 2, scaleGraphics.margin+17 + 1200 / 2));
		f.add(p);
		f.setSize(p.getWidth(), p.getHeight() + 40);
		f.setVisible(true);
		f.setLocation(800, 0);
		//f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public static void main(String[] args) {
		new XYRPlot();
	}

	static class steriodPanel extends JPanel {
		private double x = 2, y = 3, r = 0;

		@Override
		protected void paintComponent(Graphics gr) {
			super.paintComponent(gr);

			scaleGraphics g = new scaleGraphics(gr, 8.0, 12.0, getWidth(), getHeight());

			// draw robot
			g.setColor(Color.blue);
			g.fillOval(x, y,
					0.2, 0.2);

			point dir = new point(0, 0.7);
			dir.rotate(r);
			dir.translate(x, y);

			g.drawLine(x, y, dir.x, dir.y, 5);

			// print coords
			g.setColor(Color.black);
			g.setFont(new Font("Trebuchet MS", Font.PLAIN, 14));
			g.drawString(String.format("(x: %.2f y: %.2f r: %.2f)", x, y, r), 10, 20);

			// grid
			g.setColor(Color.gray);
			for (int x = 0; x < 8; x += 2) {
				g.drawLine(x, 0, x, 12);
			}
			for (int y = 0; y < 12; y += 2) {
				g.drawLine(0, y, 8, y);
			}

		}
	}
	
}
