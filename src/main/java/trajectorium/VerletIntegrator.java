package trajectorium;

import java.util.List;

public class VerletIntegrator implements Integrator{
    @Override
    public void step(List<Celestial> bodies, double dt) {
        int n = bodies.size();
        double[][] next = new double[n][2];

        for (int i = 0; i < n; i++) {
            Celestial c = bodies.get(i);
            double[] a = Celestial.acceleration(c, bodies);
            double[] p = c.getPos();
            double[] prev = c.getPrevPos();
            next[i][0] = 2 * p[0] - prev[0] + a[0] * dt * dt;
            next[i][1] = 2 * p[1] - prev[1] + a[1] * dt * dt;
        }

        for (int i = 0; i < n; i++) {
            Celestial c = bodies.get(i);
            double[] p = c.getPos();
            c.setState(next[i][0], next[i][1], p[0], p[1]);
        }
    }

    @Override
    public String getName() {
        return "Verlet";
    }
}
