import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CelestialFactoryTest {
    private static final double DT = 1.0 / 365.25;

    @Test
    void circularVelocityAtOneAuIsTwoPi() {
        assertEquals(2 * Math.PI, CelestialFactory.circularVelocity(1.0, 1.0), 1e-12);
    }

    @Test
    void massUnitsConvertToSolarMasses() {
        assertEquals(3.003e-6, CelestialFactory.MassUnit.EARTH_MASSES.toSolarMasses(1.0), 1e-8);
        assertEquals(1.0, CelestialFactory.MassUnit.KILOGRAMS.toSolarMasses(1.989e30), 1e-9);
    }

    @Test
    void createOrbitingAddsBodyWithCorrectPositionAndDirection() {
        Simulation sim = new Simulation(DT);
        CelestialFactory factory = new CelestialFactory(sim, 100);

        Celestial sun = factory.createStar("Sun", 1.0, 0, 0, 20, Color.YELLOW);
        Celestial a = factory.createOrbiting("A", 1e-6, sun, 1.0, 5, Color.BLUE, true);
        Celestial b = factory.createOrbiting("B", 1e-6, sun, 2.0, 5, Color.RED, false);

        assertEquals(3, sim.getBodies().size());
        assertEquals(1.0, a.getPos()[0], 1e-12);
        assertTrue(a.getVelocity(DT)[1] < 0);
        assertTrue(b.getVelocity(DT)[1] > 0);
    }
}
