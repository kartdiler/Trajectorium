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
    private boolean running = true;
    private double time = 0;

    private Integrator integrator = new VerletIntegrator();

    public Simulation(double dt) {
        this.dt = dt;
    }

    public void addBody(Celestial body) {
        celestials.add(body);
        log.info("Тело добавлено: {} (масса {} Солнц)", body.getName(), body.getMass());
        notifyListeners(l -> l.onBodyAdded(body));
    }

    public void removeBody(Celestial body) {
        celestials.remove(body);
        log.info("Тело удалено: {}", body.getName());
        notifyListeners(l -> l.onBodyRemoved(body));
    }

    private double stepAccumulator = 0;

    public void step() {
        if (!running) return;

        stepAccumulator += simulationSpeed;
        boolean stepped = false;
        while (stepAccumulator >= 1.0) {
            integrator.step(celestials, dt);
            time += dt;
            handleCollisions();
            stepAccumulator -= 1.0;
            stepped = true;
        }

        if (stepped) {
            notifyListeners(l -> l.onStep(this));
        }
    }

    private void handleCollisions() {
        List<Celestial> toRemove = new ArrayList<Celestial>();
        List<Celestial[]> hits = new ArrayList<Celestial[]>();

        for (int i = 0; i < celestials.size(); i++) {
            for (int j = i + 1; j < celestials.size(); j++) {
                Celestial a = celestials.get(i);
                Celestial b = celestials.get(j);

                if (toRemove.contains(a) || toRemove.contains(b)) continue;

                double[] posA = a.getPos();
                double[] posB = b.getPos();
                double distance = Math.hypot(posA[0] - posB[0], posA[1] - posB[1]);

                double collisionThreshold = a.getRealRadius() + b.getRealRadius();

                if (distance < collisionThreshold) {
                    Celestial smaller = a.getMass() < b.getMass() ? a : b;
                    Celestial bigger = (smaller == a) ? b : a;
                    toRemove.add(smaller);
                    hits.add(new Celestial[]{smaller, bigger});
                }
            }
        }

        celestials.removeAll(toRemove);

        for(Celestial[] hit : hits) {
            log.info("Столкновение: {} удалено при ударе о {}", hit[0].getName(), hit[1].getName());
            notifyListeners(l -> l.onCollision(hit[0], hit[1]));
            notifyListeners(l -> l.onBodyRemoved(hit[0]));
        }
    }

    public void setIntegrator(Integrator integrator) {
        this.integrator = integrator;
        log.info("Интегратор: {}", integrator.getName());
        notifyListeners(l -> l.onIntegratorChanged(integrator));
    }

    public Integrator getIntegrator() {
        return integrator;
    }

    public List<Celestial> getBodies() {
        return celestials;
    }

    public void pause() {
        running = false;
        log.info("Симуляция приостановлена");
        notifyListeners(l -> l.onRunningChanged(false));
    }

    public void play() {
        running = true;
        log.info("Симуляция возобновлена");
        notifyListeners(l -> l.onRunningChanged(true));
    }

    public boolean isRunning() {
        return running;
    }

    public void reset() {
        celestials.clear();
        time = 0;
        log.info("Симуляция сброшена");
        notifyListeners(l -> l.onReset());
    }

    public void setSimulationSpeed(double speed) {
        double clamped = Math.max(0, Math.min(speed, 100));
        if (clamped != this.simulationSpeed) {
            this.simulationSpeed = speed;
            log.debug("Скорость симуляции: {}", this.simulationSpeed);
            notifyListeners(l -> l.onSpeedChanged(clamped));
        }
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
