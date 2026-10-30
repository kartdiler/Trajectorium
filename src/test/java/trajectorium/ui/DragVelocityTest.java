package trajectorium.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DragVelocityTest {
    @Test
    void velocityIsDisplacementDivideByArrowTime() {
        double[] v = DragVelocity.velocity(1, 2, 1.628, 2);
        assertEquals(0.628 / DragVelocity.ARROW_YEARS, v[0], 1e-9);
        assertEquals(0, v[1], 1e-9);
    }

    @Test
    void zeroLengthDragGivesZeroVelocity() {
        double[] v = DragVelocity.velocity(3, -2, 3, -2);
        assertEquals(0, v[0], 1e-12);
        assertEquals(0, v[1], 1e-12);
    }

    @Test
    void directionFollowsTheDrag() {
        double[] v =DragVelocity.velocity(0, 0, -1, 3);
        assertTrue(v[0] < 0);
        assertTrue(v[1] > 0);
    }
}
