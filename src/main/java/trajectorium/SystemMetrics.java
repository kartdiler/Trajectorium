package trajectorium;

import java.util.List;

public class SystemMetrics {
    private SystemMetrics() {}

    public static double totalEnergy(List<Celestial> bodies, double dt) {
        double kinetic = 0;
        double potential = 0;
        for (int i = 0; i < bodies.size(); i++) {
            Celestial a = bodies.get(i);
            double[] v = a.getVelocity(dt, bodies);
            kinetic += 0.5 * a.getMass() * (v[0] * v[0] + v[1] * v[1]);

            double[] pa = a.getPos();
            for (int j = i + 1; j < bodies.size(); j++) {
                Celestial b = bodies.get(j);
                double[] pb = b.getPos();
                double r = Math.hypot(pa[0] - pb[0], pa[1] - pb[1]);
                potential -= CelestialFactory.G * a.getMass() * b.getMass() / r;
            }
        }
        return kinetic + potential;
    }

    public static double angularMomentum(List<Celestial> bodies, double dt) {
        double sum = 0;
        for (Celestial c : bodies) {
            double[] p = c.getPos();
            double[] v = c.getVelocity(dt, bodies);
            sum += c.getMass() * (p[0] * v[1] - p[1] * v[0]);
        }
        return sum;
    }

    public static double[] centerOfMass(List<Celestial> bodies) {
        double mass = 0;
        double sx = 0;
        double sy = 0;
        for (Celestial c : bodies) {
            double[] p = c.getPos();
            mass += c.getMass();
            sx += c.getMass() * p[0];
            sy += c.getMass() * p[1];
        }
        return mass == 0 ? new double[]{0, 0} : new double[]{sx / mass, sy / mass};
    }
}
