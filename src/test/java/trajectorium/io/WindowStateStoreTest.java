package trajectorium.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class WindowStateStoreTest {

    @Test
    void saveAndLoadRoundTrip(@TempDir Path dir) {
        WindowStateStore store = new WindowStateStore(dir.resolve("deep").resolve("window.json"));
        WindowState state = new WindowState(-1200, 40, 1100, 700, true);

        store.save(state);

        assertEquals(state, store.load().orElseThrow());
    }

    @Test
    void missingFileGivesNothing(@TempDir Path dir) {
        assertTrue(new WindowStateStore(dir.resolve("none.json")).load().isEmpty());
    }

    @Test
    void corruptFileIsIgnored(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("window.json");
        Files.writeString(file, "{ не json");
        assertTrue(new WindowStateStore(file).load().isEmpty());
    }

    @Test
    void unusableBoundsAreIgnoredOnLoadAndNotSaved(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("window.json");
        WindowStateStore store = new WindowStateStore(file);

        Files.writeString(file, "{\"x\":0,\"y\":0,\"width\":10,\"height\":10,\"maximized\":false}");
        assertTrue(store.load().isEmpty());

        WindowState good = new WindowState(10, 10, 900, 600, false);
        store.save(good);
        store.save(new WindowState(-32000, -32000, 160, 28, false));
        assertEquals(good, store.load().orElseThrow());
    }

    @Test
    void boundsValidationRejectsNonFiniteAndExtremeValues() {
        assertTrue(new WindowState(0, 0, 800, 600, false).boundsAreValid());
        assertFalse(new WindowState(Double.NaN, 0, 800, 600, false).boundsAreValid());
        assertFalse(new WindowState(0, 0, 20_000, 600, false).boundsAreValid());
        assertFalse(new WindowState(500_000, 0, 800, 600, false).boundsAreValid());
    }
}