import javafx.scene.paint.Color;

import java.util.List;

public class Celestial {
    private double mass;
    private String name;
    private double xPos;
    private double yPos;
    private double xPrev;
    private double yPrev;
    private double radius;
    private Color col;

    Celestial(String n, double m, double x, double y, double vx, double vy,
              double rad, Color col, List<Celestial> other, double dt) {
        this.name = n;
        this.mass = m;
        this.xPos = x;
        this.yPos = y;
        this.radius = rad;
        this.col = col;

        double[] a0 = acceleration(this, other);
        this.xPrev = x - vx * dt + a0[0] * dt * dt / 2;
        this.yPrev = y - vy * dt + a0[1] * dt * dt / 2;
    }

    static double[] acceleration(Celestial target, List<Celestial> others) {
        double ax = 0;
        double ay = 0;
        for (Celestial c: others) {
            if (c == target) continue;
            double dx = c.getPos()[0] - target.getPos()[0];
            double dy = c.getPos()[1] - target.getPos()[1];
            double r = Math.sqrt(dx * dx + dy * dy);
            double factor = 4 * Math.PI * Math.PI * c.mass / Math.pow(r, 3);
            ax += factor * dx;
            ay += factor * dy;
        }
        return new double[]{ax, ay};
    }

    void step(List<Celestial> other, double dt) {
        double[] a = acceleration(this, other);

        double xNext = 2 * xPos - xPrev + a[0] * dt * dt;
        double yNext = 2 * yPos - yPrev + a[1] * dt * dt;

        xPrev = xPos;
        yPrev = yPos;
        xPos = xNext;
        yPos = yNext;
    }

    double screenX(double origX, double scale) {
        return origX + xPos * scale;
    }

    double screenY(double origY, double scale) {
        return origY + yPos * scale;
    }

    public double getMass() {
        return mass;
    }
    public void setMass(double newMass) {
        mass = newMass;
    }

    public String getName() {
        return name;
    }
    public void setName(String newName) {
        name = newName;
    }

    public double[] getPos() {
        return new double[]{xPos, yPos};
    }
    public void setPos(double x, double y) {
        xPos = x;
        yPos = y;
    }

    public double[] getPrevPos() {
        return new double[]{xPrev, yPrev};
    }

    public double getRadius() {
        return radius;
    }
    public void setRadius(double newRad) {
        radius = newRad;
    }

    public Color getColor() { return col; }
    public void setColor(Color col) { this.col = col; }

}
