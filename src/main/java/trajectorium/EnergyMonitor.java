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
        return SystemMetrics.totalEnergy(sim.getBodies(), sim.getDt());
    }
}
