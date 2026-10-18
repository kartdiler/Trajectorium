import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationTest {
    private static final double DT = 1.0 / 365.25;

    @Test
    void pausedSimulationDoesNotMoveBodies() {
        Simulation sim = new Simulation(DT);
        Celestial body = new Celestial("Body", 1.0, 1, 0, 1, 0,
                        5, Color.WHITE, sim.getBodies(), DT, 100);

        sim.addBody(body);
        sim.pause();

        double[] before = body.getPos();
        sim.step();
        assertArrayEquals(before, body.getPos(), 1e-12);
    }

    @Test
    void simSpeedTwoMakesTwoPhysicalSteps() {
        Simulation sim = new Simulation(DT);
        Celestial body = new Celestial("Body", 1.0, 0, 0, 1, 0,
                5, Color.WHITE, sim.getBodies(), DT, 100);

        sim.addBody(body);
        sim.setSimulationSpeed(2);

        sim.step();
        assertEquals(2 * DT, body.getPos()[0], 1e-9);
    }

    @Test
    void collisionRemovesSmallerBody() {
        Simulation sim = new Simulation(DT);
        Celestial Sun = new Celestial("Sun", 1.0, 0, 0, 0, 0,
                20, Color.YELLOW, sim.getBodies(), DT, 100);
        sim.addBody(Sun);

        Celestial rock = new Celestial("Rock", 1e-6, 0.1, 0, 0, 0,
                5, Color.GRAY, sim.getBodies(), DT, 100);
        sim.addBody(rock);

        sim.step();
        assertEquals(1, sim.getBodies().size());
        assertTrue(sim.getBodies().contains(Sun));
    }

    @Test
    void simulationSpeedHalfMakesStepEveryOtherCall() {
        Simulation sim = new Simulation(DT);
        Celestial body = new Celestial("Body", 1.0, 0, 0, 1, 0,
                5, Color.WHITE, sim.getBodies(), DT, 100);
        sim.addBody(body);
        sim.setSimulationSpeed(0.5);

        sim.step();
        assertEquals(0, body.getPos()[0], 1e-12);
        sim.step();
        assertEquals(DT, body.getPos()[0], 1e-9);
    }
}
