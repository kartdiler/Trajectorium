package trajectorium;

import java.util.List;

public class EulerIntegrator implements Integrator {
    @Override
    public void step(List<Celestial> bodies, double dt) {
        int n = bodies.size();
        double[][] state = new double[n][4];

        for (int i = 0; i < n; i++) {
            Celestial c = bodies.get(i);
            double[] a = Celestial.acceleration(c, bodies);
            double[] p = c.getPos();
            double[] prev = c.getPrevPos();

            double vx = (p[0] - prev[0]) / dt;
            double vy = (p[1] - prev[1]) / dt;

            double newX = p[0] + vx * dt;
            double newY = p[1] + vy * dt;
            double newVx = vx + a[0] * dt;
            double newVy = vy + a[1] * dt;

            state[i][0] = newX;
            state[i][1] = newY;
            state[i][2] = newX - newVx * dt;
            state[i][3] = newY - newVy * dt;
        }

        for (int i = 0; i < n; i++) {
            bodies.get(i).setState(state[i][0], state[i][1], state[i][2], state[i][3]);
        }
    }

    @Override
    public String getName() {
        return "Euler";
    }
}
