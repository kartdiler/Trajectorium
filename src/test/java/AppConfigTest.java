import main.java.AppConfig;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AppConfigTest {
    @Test
    void emptyPropertiesGiveDefaults() {
        AppConfig c = new AppConfig(new Properties());
        assertEquals(1200, c.getCanvasWidth(), 1e-12);
        assertEquals(100, c.getScale(), 1e-12);
        assertEquals(1.0 / 365.25, c.getDt(), 1e-12);
        assertEquals(150, c.getTrailMaxPoints());
    }

    @Test
    void readsValuesFromProperties() {
        Properties p = new Properties();
        p.setProperty("view.scale", "200");
        p.setProperty("simulation.stepDays", "2");

        AppConfig c = new AppConfig(p);
        assertEquals(200, c.getScale(), 1e-12);
        assertEquals(2.0 / 365.25, c.getDt(), 1e-12);
    }

    @Test
    void invalidAndOutOfRangeValuesFallBackToDefaults() {
        Properties p = new Properties();
        p.setProperty("view.scale", "abc");
        p.setProperty("canvas.width", "-5");

        AppConfig c = new AppConfig(p);
        assertEquals(100, c.getScale(), 1e-12);
        assertEquals(1200, c.getCanvasWidth(), 1e-12);
    }
}
