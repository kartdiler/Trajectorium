package trajectorium.ui;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import trajectorium.Celestial;
import trajectorium.CelestialFactory;
import trajectorium.Simulation;

import static org.junit.jupiter.api.Assertions.*;

public class BodyInfoTest {
    private static final double DT = 1.0 / 365.25;

    @Test
    void earthSpeedIsAboutThirtyKmPerSecond() {
        Simulation sim = new Simulation(DT);
        CelestialFactory factory = new CelestialFactory(sim, 100);
        Celestial sun = factory.createStar("Sun", 1.0, 0, 0, 20, Color.YELLOW);
        Celestial earth = factory.createOrbiting("Earth", 3.003e-6, sun, 1.0, 5, Color.BLUE, true);

        BodyInfo info = BodyInfo.of(earth, sim);

        assertEquals(29.78, info.speedKmS(), 0.05);
        assertEquals("Sun", info.refName());
        assertEquals(1.0, info.refDistance(), 1e-9);
    }

    @Test
    void singleBodyHasNoReference() {
        Simulation sim = new Simulation(DT);
        CelestialFactory factory = new CelestialFactory(sim, 100);
        Celestial sun = factory.createStar("Sun", 1.0, 0, 0, 20, Color.YELLOW);

        BodyInfo info = BodyInfo.of(sun, sim);

        assertNull(info.refName());
        assertTrue(Double.isNaN(info.refDistance()));
    }
}
