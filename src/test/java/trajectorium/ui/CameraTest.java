package trajectorium.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CameraTest {
    private static Camera camera() {
        Camera c = new Camera(100);
        c.setViewSize(1000, 800);
        return c;
    }

    @Test
    void worldAndScreenCoordinatesRoundTrip() {
        Camera c = camera();
        c.centerOn(2, -1);
        assertEquals(500, c.toScreenX(2), 1e-9);
        assertEquals(400, c.toScreenY(-1), 1e-9);
        assertEquals(3.25, c.toWorldX(c.toScreenX(3.25)), 1e-9);
    }

    @Test
    void zoomKeepsPointUnderCursorFixed() {
        Camera c = camera();
        double wx = c.toWorldX(700);
        double wy = c.toWorldY(300);

        c.zoomAt(2, 700, 300);

        assertEquals(200, c.getScale(), 1e-9);
        assertEquals(700, c.toScreenX(wx), 1e-9);
        assertEquals(300, c.toScreenY(wy), 1e-9);
    }

    @Test
    void zoomIsClamped() {
        Camera c = camera();
        c.zoomAt(1e9, 500, 400);
        assertEquals(Camera.MAX_SCALE, c.getScale(), 1e-9);
        c.zoomAt(1e-12, 500, 400);
        assertEquals(Camera.MIN_SCALE, c.getScale(), 1e-9);
    }

    @Test
    void panMovesContentWithTheMouse() {
        Camera c = camera();
        c.panByPixels(100, 0);
        assertEquals(600, c.toScreenX(0), 1e-9);
    }

    @Test
    void fitPlacesAllPointsInsideView() {
        Camera c = camera();
        c.fit(-5, -2, 5, 2, 0.1);

        assertEquals(80, c.getScale(), 1e-9);
        assertTrue(c.toScreenX(-5) >= 0 && c.toScreenX(5) <= 1000);
        assertTrue(c.toScreenY(-2) >= 0 && c.toScreenY(2) <= 800);
    }

    @Test
    void resetRestoresDefaultView() {
        Camera c = camera();
        c.zoomAt(3, 100, 100);
        c.panByPixels(50, 50);
        c.reset();
        assertEquals(100, c.getScale(), 1e-9);
        assertEquals(500, c.toScreenX(0), 1e-9);
    }
}
