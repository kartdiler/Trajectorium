import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CelestialTest {
    private static final double DT = 1.0 / 365.25;

    @Test
    void initialVelocityMatchesConstructorInput() {
        List<Celestial> bodies = new ArrayList<Celestial>();

        Celestial body = new Celestial("Test", 1.0, 1, 0, 0, 2 * Math.PI,
                5, Color.WHITE, bodies, DT, 100);
        double[] v = body.getVelocity(DT);
        assertEquals(0, v[0], 1e-9);
        assertEquals(2 * Math.PI, v[1], 1e-9);
    }

    @Test
    void accelerationIsZeroWithoutOtherBodies() {
        Celestial one = new Celestial("One", 1.0, 1, 0, 0, 0,
                5, Color.WHITE, List.of(), DT, 100);
        double[] a = Celestial.acceleration(one, List.of());
        assertEquals(0, a[0], 1e-12);
        assertEquals(0, a[1], 1e-12);
    }

    @Test
    void accelerationPointsTowardAttractor() {
        List<Celestial> system =  new ArrayList<Celestial>();

        Celestial Sun = new Celestial("Sun", 1.0, 0, 0, 0, 0,
                20, Color.YELLOW, system, DT, 100);
        system.add(Sun);

        Celestial Earth = new Celestial("Earth", 3.003e-6, 1, 0, 0, 2 * Math.PI,
                        5, Color.BLUE, system, DT, 100);

        double[] a = Celestial.acceleration(Earth, system);
        assertTrue(a[0] < 0);
        assertEquals(0, a[1], 1e-9);
    }

    @Test
    void accelerationMagnitudeMatchesNewtonLaw() {
        List<Celestial> system =  new ArrayList<Celestial>();
        Celestial Sun = new Celestial("Sun", 1.0, 0, 0, 0, 0,
                20, Color.YELLOW, system, DT, 100);
        system.add(Sun);

        Celestial Planet = new Celestial("Planet", 1e-6, 2, 0, 0, 0,
                5, Color.BLUE, system, DT, 100);

        double[] a = Celestial.acceleration(Planet, system);
        double expected = -1 * 4 * Math.PI * Math.PI * 1.0 / (2.0 * 2.0);
        assertEquals(expected, a[0], 1e-9);
    }

    @Test
    void newtonThirdLawHolds() {
        List<Celestial> system = new ArrayList<Celestial>();

        Celestial a = new Celestial("A", 2.0, 0, 0, 0, 0,
                5, Color.RED, system, DT, 100);

        Celestial b = new Celestial("B", 0.5, 1, 0, 0, 0,
                5, Color.BLUE, system, DT, 100);

        Collections.addAll(system, a, b);

        double[] aa = Celestial.acceleration(a, system);
        double[] ab = Celestial.acceleration(b, system);

        assertEquals(0, a.getMass() * aa[0] + b.getMass() * ab[0], 1e-9);
        assertEquals(0, a.getMass() * aa[1] + b.getMass() * ab[1], 1e-9);
    }

    @Test
    void freeBodyMovesInStraightLine() {
        Celestial body = new Celestial("Body", 1.0, 0, 0, 1.0, 0.5,
                5, Color.WHITE, List.of(), DT, 100);

        for (int i = 0; i < 100; i++) {
            body.step(List.of(), DT);
        }

        double[] pos = body.getPos();
        assertEquals(1.0 * 100 * DT, pos[0], 1e-9);
        assertEquals(0.5 * 100 * DT, pos[1], 1e-9);
    }
}
