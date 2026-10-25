package trajectorium;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
