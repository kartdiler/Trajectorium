package trajectorium;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class PlacementTest {
    private static Simulation sceneWithSun() {
        Simulation sim = new Simulation(1.0 / 365.25);
        new CelestialFactory(sim, 100).createStar("Sun", 1.0, 0, 0, 20, Color.YELLOW);
        return sim;
    }

    @Test
    void detectsBodyInsideAnotherBody() {
        Optional<Celestial> hit = Placement.findOverlap(sceneWithSun().getBodies(), 0.1, 0, 0.05);
        assertTrue(hit.isPresent());
        assertEquals("Sun", hit.get().getName());
    }

    @Test
    void detectsOverlapOfRadiiEvenIfCentreIsOutside() {
        assertTrue(Placement.findOverlap(sceneWithSun().getBodies(), 0.22, 0, 0.05).isPresent());
    }

    @Test
    void acceptsFreePlaceAndEmptyScene() {
        assertTrue(Placement.findOverlap(sceneWithSun().getBodies(), 1.0, 0, 0.05).isEmpty());
        assertTrue(Placement.findOverlap(new Simulation(1.0 / 365.25).getBodies(), 0, 0, 0.05).isEmpty());
    }

    @Test
    void returnsTheFirstOverlappingBody() {
        Simulation sim = sceneWithSun();
        new CelestialFactory(sim, 100).createStar("Twin", 1.0, 5, 0, 20, Color.RED);

        assertEquals("Twin", Placement.findOverlap(sim.getBodies(), 5.0, 0.1, 0.05).orElseThrow().getName());
    }
}