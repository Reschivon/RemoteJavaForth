package ReplGraphics.XYRPlot;

import java.awt.*;
import java.awt.geom.Line2D;

public class scaleGraphics {
    Graphics g;
    public double sx, sy, height;
    public static int margin = 0;

    public scaleGraphics(Graphics g, double x_units, double y_units, double width, double height){
        this.g = g;
        height -= margin;
        width -= margin;
        this.height = height;
        this.sx = width / x_units * 1.0;
        this.sy = height / y_units * 1.0;


        ((Graphics2D)g).setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        ((Graphics2D)g).setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
    }

    public void fillOval(double x, double y, double width, double height){
        width = Math.abs(width);
        height = Math.abs(height);
        g.fillOval(margin + (int)(sx * (x - width)), margin + (int)(this.height - sy * (y + height)), (int)(width * sx * 2), (int)(height * sy * 2));
    }public void drawOval(double x, double y, double width, double height){
        width = Math.abs(width);
        height = Math.abs(height);
        g.drawOval(margin + (int)(sx * (x - width)), margin + (int)(this.height - sy * (y + height)), (int)(width * sx * 2), (int)(height * sy * 2));
    }
    public void drawArc(double x, double y, double width, double height, double startAng, double endAng){
        width = Math.abs(width);
        height = Math.abs(height);

        g.drawArc(
                margin + (int)(sx * (x - width)), margin + (int)(this.height - sy * (y + height)),
                (int)(width * sx * 2), (int)(height * sy * 2),
                (int)startAng, (int)endAng);
    }

    public void setColor(Color x){
        g.setColor(x);
    }
    public void setFont(Font f){
        g.setFont(f);
    }
    public void drawString(String s, double x, double y){
        g.drawString(s, (int)(x), (int)(y));
    }public void drawStringScaled(String s, double x, double y){
        g.drawString(s, margin + (int)(x * sx), margin + (int)(height - y * sy));
    }
    public void drawLine(double x0, double y0, double x1, double y1){
        drawLine(x0, y0, x1, y1, 1);
    }
    public void drawLine(double x0, double y0, double x1, double y1, int stroke){
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(stroke));
        g2.draw(new Line2D.Float(margin + (int)(x0 * sx), margin + (int)(height - y0 * sy), margin + (int)(x1 * sx), margin + (int)(height - y1 * sy)));
    }
}
