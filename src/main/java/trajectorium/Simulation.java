package trajectorium;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Simulation {
    private static final Logger log = LoggerFactory.getLogger(Simulation.class);

    private final List<SimulationListener> listeners = new CopyOnWriteArrayList<SimulationListener>();
    private final List<Celestial> celestials = new ArrayList<Celestial>();
    private double dt;
    private double simulationSpeed = 1.0;
    private boolean isRunning = true;
    private double time = 0;

    private Integrator integrator = new VerletIntegrator();

    public Simulation(double dt) {
        this.dt = dt;
    }

    public void addBody(Celestial body) {
        celestials.add(body);
        log.info("Тело добавлено: {} (масса {} Солнц)", body.getName(), body.getMass());
    }

    public void removeBody(Celestial body) {
        celestials.remove(body);
        log.info("Тело удалено: {}", body.getName());
    }

    private double stepAccumulator = 0;

    public void step() {
        if (!isRunning) return;

        stepAccumulator += simulationSpeed;

        while (stepAccumulator >= 1.0) {
            integrator.step(celestials, dt);
            handleCollisions();

            stepAccumulator -= 1.0;
        }
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
                    Celestial bigger = (smaller == a) ? b : a;
                    toRemove.add(smaller);
                    log.info("Столкновение: {} удалено при ударе о {}", smaller.getName(), bigger.getName());
                }
            }
        }

        celestials.removeAll(toRemove);
    }

    public void setIntegrator(Integrator integrator) {
        this.integrator = integrator;
        log.info("Интегратор: {}", integrator.getName());
    }

    public Integrator getIntegrator() {
        return integrator;
    }

    public List<Celestial> getBodies() {
        return celestials;
    }

    public void pause() {
        isRunning = false;
        log.info("Симуляция приостановлена");
    }

    public void play() {
        isRunning = true;
        log.info("Симуляция возобновлена");
    }

    public void reset() {
        celestials.clear();
        log.info("Симуляция сброшена");
    }

    public void setSimulationSpeed(double speed) {
        this.simulationSpeed = speed;
        log.debug("Скорость симуляции: {}", this.simulationSpeed);
    }

    public double getSimulationSpeed() {
        return simulationSpeed;
    }

    public double getDt() {
        return dt;
    }

    public void addListener(SimulationListener l) {
        listeners.add(l);
    }

    public void removeListener(SimulationListener l) {
        listeners.remove(l);
    }

    public double getTime() {
        return time;
    }

    public void notifyListeners(Consumer<SimulationListener> event) {
        for (SimulationListener l : listeners) {
            try {
                event.accept(l);
            } catch (RuntimeException ex) {
                log.error("Ошибка в слушателе {}", l, ex);
            }
        }
    }
}
