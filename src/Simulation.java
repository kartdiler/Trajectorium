import java.util.ArrayList;
import java.util.List;

public class Simulation {
    private final List<Celestial> celestials = new ArrayList<Celestial>();
    private double dt;
    private double simulationSpeed = 1.0;
    private boolean isRunning = true;

    public Simulation(double dt) {
        this.dt = dt;
    }

    public void addBody(Celestial body) {
        celestials.add(body);
    }

    public void removeBody(Celestial body) {
        celestials.remove(body);
    }

    private double stepAccumulator = 0;

    public void step() {
        if (!isRunning) return;

        stepAccumulator += simulationSpeed;

        while (stepAccumulator >= 1.0) {
            for (Celestial cel : celestials) {
                cel.step(celestials, dt);
            }
            handleCollisions();

            stepAccumulator -= 1.0;
        }

        for (Celestial cel : celestials) {
            System.out.print(cel.getName() + " ");
        }
        System.out.println();
    }

    private void handleCollisions() {
        List<Celestial> toRemove = new ArrayList<Celestial>();

        for (int i = 0; i < celestials.size(); i++) {
            for (int j = i + 1; j < celestials.size(); j++) {
                Celestial a = celestials.get(i);
                Celestial b = celestials.get(j);

                if (toRemove.contains(a) || toRemove.contains(b)) continue;

                double[] posA = a.getPos();
                double[] posB = b.getPos();

                double dx = posA[0] - posB[0];
                double dy = posA[1] - posB[1];
                double distance = Math.sqrt(dx * dx + dy * dy);

                double collisionThreshold = a.getRealRadius() + b.getRealRadius();

                if (distance < collisionThreshold) {
                    Celestial smaller = a.getMass() < b.getMass() ? a : b;
                    toRemove.add(smaller);
                }
            }
        }

        celestials.removeAll(toRemove);
    }

    public List<Celestial> getBodies() {
        return celestials;
    }

    public void pause() {
        isRunning = false;
    }

    public void play() {
        isRunning = true;
    }

    public void reset() {
        celestials.clear();
    }

    public void setSimulationSpeed(double speed) {
        this.simulationSpeed = speed;
    }

    public double getSimulationSpeed() {
        return simulationSpeed;
    }

    public double getDt() {
        return dt;
    }
}
