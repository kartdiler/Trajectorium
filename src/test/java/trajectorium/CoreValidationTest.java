package trajectorium;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class CoreValidationTest {
    private static final double DT = 1.0 / 365.25;

    private static void assertRejected(String parameter, org.junit.jupiter.api.function.Executable action) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, action);
        assertTrue(e.getMessage().contains(parameter), "в сообщении нет «" + parameter + "»: " + e.getMessage());
    }

    private static Celestial create(String name, double mass, double x, double y, double vx, double vy,
                                    double radius, Color color, List<Celestial> others, double dt, double scale) {
        return new Celestial(name, mass, x, y, vx, vy, radius, color, others, dt, scale);
    }

    @Test
    void validBodyIsCreated() {
        assertDoesNotThrow(() -> create("Earth", 3.003e-6, 1, 0, 0, 6.28, 5, Color.BLUE, new ArrayList<>(), DT, 100));
    }

    @Test
    void celestialRejectsEveryInvalidParameter() {
        List<Celestial> none = new ArrayList<>();
        assertRejected("имя тела", () -> create("", 1, 0, 0, 0, 0, 5, Color.RED, none, DT, 100));
        assertRejected("имя тела", () -> create("x".repeat(51), 1, 0, 0, 0, 0, 5, Color.RED, none, DT, 100));
        assertRejected("масса", () -> create("A", 0, 0, 0, 0, 0, 5, Color.RED, none, DT, 100));
        assertRejected("масса", () -> create("A", -1, 0, 0, 0, 0, 5, Color.RED, none, DT, 100));
        assertRejected("масса", () -> create("A", 2e6, 0, 0, 0, 0, 5, Color.RED, none, DT, 100));
        assertRejected("координата x", () -> create("A", 1, Double.NaN, 0, 0, 0, 5, Color.RED, none, DT, 100));
        assertRejected("координата y", () -> create("A", 1, 0, 2e6, 0, 0, 5, Color.RED, none, DT, 100));
        assertRejected("скорость vx", () -> create("A", 1, 0, 0, Double.POSITIVE_INFINITY, 0, 5, Color.RED, none, DT, 100));
        assertRejected("скорость vy", () -> create("A", 1, 0, 0, 0, -2e6, 5, Color.RED, none, DT, 100));
        assertRejected("радиус", () -> create("A", 1, 0, 0, 0, 0, 0, Color.RED, none, DT, 100));
        assertRejected("радиус", () -> create("A", 1, 0, 0, 0, 0, 101, Color.RED, none, DT, 100));
        assertRejected("цвет", () -> create("A", 1, 0, 0, 0, 0, 5, null, none, DT, 100));
        assertRejected("список тел", () -> create("A", 1, 0, 0, 0, 0, 5, Color.RED, null, DT, 100));
        assertRejected("шаг", () -> create("A", 1, 0, 0, 0, 0, 5, Color.RED, none, 0, 100));
        assertRejected("масштаб", () -> create("A", 1, 0, 0, 0, 0, 5, Color.RED, none, DT, -1));
    }

    @Test
    void boundaryValuesAreAccepted() {
        List<Celestial> none = new ArrayList<>();
        assertDoesNotThrow(() -> create("A", Limits.MAX_ABS, Limits.MAX_ABS, -Limits.MAX_ABS,
                Limits.MAX_ABS, -Limits.MAX_ABS, Limits.MAX_RADIUS_PX, Color.RED, none, DT, 100));
        assertDoesNotThrow(() -> create("x".repeat(Limits.MAX_NAME_LENGTH), 1e-19, 0, 0, 0, 0, 0.1,
                Color.RED, none, DT, 100));
    }

    @Test
    void settersValidateToo() {
        Celestial body = create("A", 1, 0, 0, 0, 0, 5, Color.RED, new ArrayList<>(), DT, 100);
        assertRejected("масса", () -> body.setMass(-5));
        assertRejected("координата x", () -> body.setPos(Double.NaN, 0));
        assertEquals(1.0, body.getMass(), 0.0);
        assertEquals(0.0, body.getPos()[0], 0.0);
    }

    @Test
    void factoryRejectsInvalidArguments() {
        Simulation sim = new Simulation(DT);
        CelestialFactory factory = new CelestialFactory(sim, 100);
        Celestial sun = factory.createStar("Sun", 1.0, 0, 0, 20, Color.YELLOW);

        assertRejected("симуляция", () -> new CelestialFactory(null, 100));
        assertRejected("масштаб", () -> new CelestialFactory(sim, 0));
        assertRejected("длина следа", () -> factory.setTrailMaxPoints(-1));
        assertRejected("родительское тело",
                () -> factory.createOrbiting("A", 1e-6, null, 1.0, 5, Color.BLUE, true));
        assertRejected("радиус орбиты",
                () -> factory.createOrbiting("A", 1e-6, sun, 0, 5, Color.BLUE, true));
        assertRejected("единица массы",
                () -> factory.createFree("A", 1, null, 3, 0, 0, 0, 5, Color.BLUE));
        assertRejected("масса",
                () -> factory.createFree("A", -1, CelestialFactory.MassUnit.SOLAR_MASSES, 3, 0, 0, 0, 5, Color.BLUE));
        assertEquals(1, sim.getBodies().size());
    }

    @Test
    void simulationRejectsInvalidArgumentsAndKeepsItsState() {
        assertRejected("шаг", () -> new Simulation(0));
        assertRejected("шаг", () -> new Simulation(Double.NaN));

        Simulation sim = new Simulation(DT);
        AtomicInteger events = new AtomicInteger();
        sim.addListener(new SimulationListener() {
            @Override public void onBodyAdded(Celestial body) { events.incrementAndGet(); }
        });
        Celestial body = create("Sun", 1, 0, 0, 0, 0, 20, Color.YELLOW, sim.getBodies(), DT, 100);
        sim.addBody(body);

        assertRejected("тело", () -> sim.addBody(null));
        assertRejected("Sun", () -> sim.addBody(body));
        assertRejected("интегратор", () -> sim.setIntegrator(null));
        assertRejected("подписчик", () -> sim.addListener(null));
        assertRejected("NaN", () -> sim.setSimulationSpeed(Double.NaN));

        assertEquals(1, sim.getBodies().size());
        assertEquals(1, events.get());
    }

    @Test
    void simulationSpeedIsClampedNotRejected() {
        Simulation sim = new Simulation(DT);
        sim.setSimulationSpeed(500);
        assertEquals(100, sim.getSimulationSpeed(), 0.0);
        sim.setSimulationSpeed(-5);
        assertEquals(0, sim.getSimulationSpeed(), 0.0);
    }

    @Test
    void trailRejectsNegativeLimit() {
        assertRejected("число точек", () -> new Trail(-1));
        assertDoesNotThrow(() -> new Trail(0));
    }
}