package test.java;

import main.java.Trail;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TrailTest {
    @Test
    void trailRespectsMaxPoints() {
        Trail trail = new Trail(3);
        for (int i = 1; i <= 4; i++) {
            trail.addPoint(i, i);
        }

        Assertions.assertEquals(3, trail.getPoints().size());
        Assertions.assertEquals(2.0, trail.getPoints().peekFirst()[0], 1e-12);
    }

    @Test
    void disabledTrailClearsAndIgnoresPoints() {
        Trail trail = new Trail(10);
        trail.addPoint(1, 1);
        trail.setEnabled(false);
        trail.addPoint(2, 2);
        Assertions.assertTrue(trail.getPoints().isEmpty());
    }
}
