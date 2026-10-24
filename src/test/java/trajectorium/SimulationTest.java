package trajectorium;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.classic.Logger;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

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
        Assertions.assertEquals(1, sim.getBodies().size());
        Assertions.assertTrue(sim.getBodies().contains(Sun));
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

    @Test
    void collisionIsLogged() {
        Logger log = (Logger) LoggerFactory.getLogger(Simulation.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<ILoggingEvent>();
        appender.start();
        log.addAppender(appender);

        try {
            Simulation sim = new Simulation(DT);

            Celestial Sun = new Celestial("Sun", 1.0, 0, 0, 0, 0,
                    20, Color.YELLOW, sim.getBodies(), DT, 100);
            sim.addBody(Sun);

            Celestial rock = new Celestial("Rock", 1e-6, 0.1, 0, 0, 0,
                    5, Color.GRAY, sim.getBodies(), DT, 100);
            sim.addBody(rock);

            sim.step();

            assertTrue(appender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("Столкновение")));
        } finally {
            log.detachAppender(appender);
        }
    }

    @Test
    void integratorCanBeSwapped() {
        Simulation sim = new Simulation(DT);
        sim.setIntegrator(new EulerIntegrator());
        assertEquals("Euler", sim.getIntegrator().getName());
    }
}
