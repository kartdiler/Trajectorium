package trajectorium;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class EnergyMonitor implements SimulationListener {
    private final Deque<double[]> history = new ArrayDeque<double[]>();
    private final int maxPoints;

    public EnergyMonitor(int maxPoints) {
        this.maxPoints = maxPoints;
    }

    @Override
    public void onStep(Simulation sim) {
        history.addLast(new double[] {sim.getTime(), totalEnergy(sim)});
        if (history.size() > maxPoints) {
            history.removeFirst();
        }
    }

    @Override
    public void onReset() {
        history.clear();
    }

    public Deque<double[]> getHistory() {
        return history;
    }

    static double totalEnergy(Simulation sim) {
        List<Celestial> bodies = sim.getBodies();
        double kinetic = 0;
        double potential = 0;

        for (int i = 0; i < bodies.size(); i++) {
            Celestial a = bodies.get(i);
            double[] v = a.getVelocity(sim.getDt());
            kinetic += 0.5 * a.getMass() * (v[0] * v[0] + v[1] * v[1]);
            for (int j = i + 1; j < bodies.size(); j++) {
                Celestial b = bodies.get(j);
                double[] pa = a.getPos();
                double[] pb = b.getPos();

                double r = Math.hypot(pa[0] - pb[0], pa[1] - pb[1]);
                potential -= CelestialFactory.G * a.getMass() * b.getMass();
            }
        }

        return kinetic + potential;
    }
}
