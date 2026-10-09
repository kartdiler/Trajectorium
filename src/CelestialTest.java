import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CelestialTest {
    @Test
    void initialVelocityMatchesConstructorInput() {
        List<Celestial> bodies = new ArrayList<Celestial>();

        Celestial body = new Celestial("Test", 1.0, 1, 0, 0, 2 * Math.PI,
                5, Color.WHITE, bodies, 1.0/365.25, 100);
        double[] v = body.getVelocity(1.0/365.25);
        assertEquals(0, v[0], 1e-9);
    }

    @Test
    void accelerationIsZeroWithoutOtherBodies() {
        Celestial one = new Celestial("One", 1.0, 1, 0, 0, 0,
                5, Color.WHITE, List.of(), 1.0/365.25, 100);
        double[] a = Celestial.acceleration(one, List.of());
        assertEquals(0, a[0], 1e-12);
        assertEquals(0, a[1], 1e-12);
    }
}
