package trajectorium.io;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import trajectorium.Celestial;
import trajectorium.CelestialFactory;
import trajectorium.Simulation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SceneMapperTest {
    private static final double DT = 1.0 / 365.25;

    private static Simulation buildSystem() {
        Simulation sim = new Simulation(DT);
        CelestialFactory factory = new CelestialFactory(sim, 100);
        Celestial sun = factory.createStar("Sun", 1.0, 0, 0, 20, Color.YELLOW);
        factory.createOrbiting("Earth", 3.003e-6, sun, 1.0, 5, Color.BLUE, true);
        Celestial jupiter = factory.createOrbiting("Jupiter", 9.55e-4, sun, 5.2, 6,
                Color.ORANGERED, true);
        factory.createOrbiting("Moon", 3.69e-8, jupiter, 0.11257, 2, Color.WHITE, true);
        return sim;
    }

    private static Simulation copyOf(Simulation original) {
        Simulation copy = new Simulation(DT);
        SceneMapper.apply(SceneMapper.capture(original), copy, new CelestialFactory(copy, 100));
        return copy;
    }

    @Test
    void captureAndApplyRestoreExactState() {
        Simulation original = buildSystem();
        Simulation copy = copyOf(original);

        assertEquals(original.getBodies().size(), copy.getBodies().size());
        for (int i = 0; i < original.getBodies().size(); i++) {
            Celestial a = original.getBodies().get(i);
            Celestial b = copy.getBodies().get(i);
            assertEquals(a.getName(), b.getName());
            assertArrayEquals(a.getPos(), b.getPos(), 1e-12);
            assertArrayEquals(a.getPrevPos(), b.getPrevPos(), 1e-12);
        }
    }

    @Test
    void restoredSystemEvolvesIdentically() {
        Simulation original = buildSystem();
        Simulation copy = copyOf(original);

        for (int i = 0; i < 200; i++) {
            original.step();
            copy.step();
        }

        for (int i = 0; i < original.getBodies().size(); i++) {
            assertArrayEquals(original.getBodies().get(i).getPos(), copy.getBodies().get(i).getPos(), 1e-9);
        }
    }

    @Test
    void jsonRoundTripKeepsEverything(@TempDir Path dir) throws Exception {
        SceneData data = SceneMapper.capture(buildSystem());
        Path file = dir.resolve("scene.json");

        SceneStorage.save(file, data);

        assertEquals(data, SceneStorage.load(file));
    }

    @Test
    void brokenJsonIsRejected(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("bad.json");
        Files.writeString(file, "{ это не json");
        assertThrows(SceneFormatException.class, () -> SceneStorage.load(file));
    }

    @Test
    void emptyObjectIsRejected(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("empty.json");
        Files.writeString(file, "{}");
        assertThrows(SceneFormatException.class, () -> SceneStorage.load(file));
    }

    @Test
    void missingFileIsAnIoError(@TempDir Path dir) {
        assertThrows(IOException.class, () -> SceneStorage.load(dir.resolve("nothing.json")));
    }

    @Test
    void binaryStarPresetLoadsAndStaysStable() throws Exception {
        SceneData data = SceneStorage.loadResource("/scenes/binary-star.json");
        Simulation sim = new Simulation(DT);
        SceneMapper.apply(data, sim, new CelestialFactory(sim, 100));
        Celestial a = sim.getBodies().get(0);
        Celestial b = sim.getBodies().get(1);

        double maxDeviation = 0;
        for (int i = 0; i < 400; i++) {
            sim.step();
            double[] pa = a.getPos();
            double[] pb = b.getPos();
            maxDeviation = Math.max(maxDeviation, Math.abs(Math.hypot(pa[0] - pb[0], pa[1] - pb[1]) - 2.0));
        }

        assertTrue(maxDeviation < 1e-2, "расстояние между звездами ушло на " + maxDeviation);
    }

    private static BodyData body(String name, double mass, double x) {
        return new BodyData(name, mass, x, 0, 0, 0, 5, "#FFFFFF");
    }

    private static SceneData scene(BodyData... bodies) {
        return new SceneData(SceneData.CURRENT_VERSION, "Verlet", 1.0, List.of(bodies));
    }

    @Test
    void validSceneIsAccepted() {
        assertDoesNotThrow(() -> SceneValidator.validate(scene(body("A", 1, 0), body("B", 1, 3))));
    }

    @Test
    void invalidSceneParametersAreRejected() {
        assertThrows(SceneFormatException.class, () -> SceneValidator.validate(
                new SceneData(2, "Verlet", 1.0, List.of())
        ));
        assertThrows(SceneFormatException.class, () -> SceneValidator.validate(
                new SceneData(1, "RK4", 1.0, List.of())
        ));
        assertThrows(SceneFormatException.class, () -> SceneValidator.validate(
                new SceneData(1, "Verlet", 1.0, null)
        ));
    }

    @Test
    void invalidBodiesAreRejected() {
        assertThrows(SceneFormatException.class, () -> SceneValidator.validate(scene(body("A", 0, 0))));
        assertThrows(SceneFormatException.class, () -> SceneValidator.validate(scene(body("A", 1, Double.NaN))));
        assertThrows(SceneFormatException.class, () -> SceneValidator.validate(scene(body("", 1, 0))));
        assertThrows(SceneFormatException.class, () -> SceneValidator.validate(scene(new BodyData(
                "A", 1, 0, 0, 0, 0, 5, "red"))));
        assertThrows(SceneFormatException.class, () -> SceneValidator.validate(scene(
                body("A", 1, 0), body("B", 1, 0))));
    }

    @Test
    void tooManyBodiesAreRejected() {
        BodyData[] many = new BodyData[SceneValidator.MAX_BODIES + 1];
        for (int i = 0; i < many.length; i++) {
            many[i] = body("B" + i, 1, i * 10.0);
        }
        assertThrows(SceneFormatException.class, () -> SceneValidator.validate(scene(many)));
    }
}
