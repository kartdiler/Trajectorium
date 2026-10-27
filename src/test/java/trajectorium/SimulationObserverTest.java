package trajectorium;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimulationObserverTest {
    private static final double DT = 1.0 / 365.25;

    @Test
    void onStepIsCalledOncePerFrame() {
        Simulation sim = new Simulation(DT);
        AtomicInteger calls = new AtomicInteger();

        sim.addListener(new SimulationListener() {
            @Override
            public void onStep(Simulation sim) {
                calls.incrementAndGet();
            }
        });
        sim.setSimulationSpeed(3);

        sim.step();

        assertEquals(1, calls.get());
        assertEquals(3 * DT, sim.getTime(), 1e-12);
    }

    @Test
    void pausedSimulationDoesNotNotify() {
        Simulation sim = new Simulation(DT);
        AtomicInteger calls = new AtomicInteger();

        sim.addListener(new SimulationListener() {
            @Override
            public void onStep(Simulation sim) {
                calls.incrementAndGet();
            }
        });
        sim.pause();

        sim.step();

        assertEquals(0, calls.get());
    }

    @Test
    void bodyAddedAndCollisionAreReported() {
        Simulation sim = new Simulation(DT);
        List<String> events = new ArrayList<String>();

        sim.addListener(new SimulationListener() {
            @Override
            public void onBodyAdded(Celestial body) {
                events.add("added:" + body.getName());
            }

            @Override
            public void onBodyRemoved(Celestial body) {
                events.add("removed:" + body.getName());
            }

            @Override
            public void onCollision(Celestial removed, Celestial stayed) {
                events.add("collision:" + removed.getName() + "->" + stayed.getName());
            }
        });

        sim.addBody(new Celestial("Sun", 1.0, 0, 0, 0, 0,
                20, Color.YELLOW, sim.getBodies(), DT, 100));
        sim.addBody(new Celestial("Rock", 1e-6, 0.1, 0, 0, 0,
                5, Color.GRAY, sim.getBodies(), DT, 100));
        sim.step();

        assertEquals(List.of("added:Sun", "added:Rock", "collision:Rock->Sun", "removed:Rock"), events);
    }

    @Test
    void removedListenerIsNotNotified() {
        Simulation sim = new Simulation(DT);
        AtomicInteger calls = new AtomicInteger();
        SimulationListener l = new SimulationListener() {
            @Override
            public void onStep(Simulation sim) {
                calls.incrementAndGet();
            }
        };

        sim.addListener(l);
        sim.step();
        sim.removeListener(l);
        sim.step();

        assertEquals(1, calls.get());
    }

    @Test
    void failingListenerDoesNotBreakOthers() {
        Simulation sim = new Simulation(DT);
        AtomicInteger calls = new AtomicInteger();

        sim.addListener(new SimulationListener() {
            @Override
            public void onStep(Simulation sim) {
                throw new RuntimeException("TEST");
            }
        });
        sim.addListener(new SimulationListener() {
            @Override
            public void onStep(Simulation sim) {
                calls.incrementAndGet();
            }
        });

        sim.step();

        assertEquals(1, calls.get());
    }

    @Test
    void energyMonitorRecordsHistoryAndClearOnReset() {
        Simulation sim = new Simulation(DT);
        EnergyMonitor monitor = new EnergyMonitor(10);

        sim.addListener(monitor);
        sim.addBody(new Celestial("Sun", 1.0, 0, 0, 0, 0,
                        20, Color.YELLOW, sim.getBodies(), DT, 100));

        for (int i = 0; i < 15; i++) {
            sim.step();
        }
        assertEquals(10, monitor.getHistory().size());

        sim.reset();
        assertTrue(monitor.getHistory().isEmpty());
    }

    @Test
    void speedChangeIsReportedOnlyWhenValueChanges() {
        Simulation sim = new Simulation(DT);
        List<Double> speeds = new ArrayList<Double>();
        sim.addListener(new SimulationListener() {
            @Override
            public void onSpeedChanged(double speed) {
                speeds.add(speed);
            }
        });

        sim.setSimulationSpeed(2);
        sim.setSimulationSpeed(2);
        sim.setSimulationSpeed(500);

        assertEquals(List.of(2.0, 100.0), speeds);
    }
}
