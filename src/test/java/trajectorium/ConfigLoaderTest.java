package trajectorium;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigLoaderTest {

    private static ConfigLoader.ResourceOpener resources(Map<String, String> files) {
        return path -> files.containsKey(path)
                ? new ByteArrayInputStream(files.get(path).getBytes(StandardCharsets.UTF_8))
                : null;
    }

    private static ConfigLoader.Loaded load(List<String> args, Map<String, String> env, Path user, Map<String, String> files) {
        return ConfigLoader.load(args, env, user, resources(files));
    }

    private static String scale(ConfigLoader.Loaded loaded) {
        return loaded.properties().getProperty("view.scale");
    }

    @Test
    void noSourcesMeansDefaultsAndAWarning(@TempDir Path dir) {
        ConfigLoader.Loaded loaded = load(List.of(), Map.of(), dir.resolve("none.properties"), Map.of());

        assertTrue(loaded.properties().isEmpty());
        assertTrue(loaded.warnings().stream().anyMatch(w -> w.contains("config.properties")));
        assertEquals(100.0, new AppConfig(loaded.properties()).getScale(), 0.0);
    }

    @Test
    void sourcesOverrideEachOtherInPriorityOrder(@TempDir Path dir) throws IOException {
        Map<String, String> files = new HashMap<>();
        files.put("/config.properties", "view.scale=100");
        files.put("/config-demo.properties", "view.scale=110");
        Path user = dir.resolve("config.properties");
        Files.writeString(user, "view.scale=120");
        Path noUser = dir.resolve("absent.properties");
        Map<String, String> envAll = Map.of("TRAJECTORIUM_VIEW_SCALE", "130", "TRAJECTORIUM_PROFILE", "demo");
        Map<String, String> envProfile = Map.of("TRAJECTORIUM_PROFILE", "demo");

        ConfigLoader.Loaded args = load(List.of("--view.scale=140"), envAll, user, files);
        assertEquals("140", scale(args));
        assertEquals("аргумент --view.scale", args.sources().get("view.scale"));

        assertEquals("130", scale(load(List.of(), envAll, user, files)));
        assertEquals("120", scale(load(List.of(), envProfile, user, files)));
        assertEquals("110", scale(load(List.of(), envProfile, noUser, files)));
        assertEquals("100", scale(load(List.of(), Map.of(), noUser, files)));
    }

    @Test
    void profileFromArgumentBeatsProfileFromEnvironment(@TempDir Path dir) {
        Map<String, String> files = Map.of(
                "/config-a.properties", "view.scale=11",
                "/config-b.properties", "view.scale=22");

        ConfigLoader.Loaded loaded = load(List.of("--profile=b"), Map.of("TRAJECTORIUM_PROFILE", "a"),
                dir.resolve("none"), files);

        assertEquals("22", scale(loaded));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../secret", "A", "a b", "", "xxxxxxxxxxxxxxxxxxxxx", "a/b"})
    void unsafeProfileNamesAreRejected(String name, @TempDir Path dir) {
        ConfigLoader.Loaded loaded = load(List.of("--profile=" + name), Map.of(), dir.resolve("none"), Map.of());

        assertTrue(loaded.properties().isEmpty());
        if (!name.isEmpty()) {
            assertTrue(loaded.warnings().stream().anyMatch(w -> w.contains("имя профиля")), loaded.warnings().toString());
        }
    }

    @Test
    void missingProfileIsReported(@TempDir Path dir) {
        ConfigLoader.Loaded loaded = load(List.of("--profile=ghost"), Map.of(), dir.resolve("none"), Map.of());
        assertTrue(loaded.warnings().stream().anyMatch(w -> w.contains("ghost")));
    }

    @Test
    void unknownKeysAndArgumentsAreReportedNotApplied(@TempDir Path dir) throws IOException {
        Path user = dir.resolve("config.properties");
        Files.writeString(user, "view.scale=150\nview.skale=999\nprofile=demo\n");

        ConfigLoader.Loaded loaded = load(List.of("--zoom=3", "hello", "--print-config"), Map.of(), user, Map.of());

        assertEquals("150", scale(loaded));
        assertNull(loaded.properties().getProperty("view.skale"));
        String warnings = String.join("\n", loaded.warnings());
        assertTrue(warnings.contains("view.skale"), warnings);
        assertTrue(warnings.contains("\"profile\""), warnings);
        assertTrue(warnings.contains("--zoom"), warnings);
        assertTrue(warnings.contains("hello"), warnings);
        assertFalse(warnings.contains("--print-config"), warnings);
    }

    @Test
    void valuesAreTrimmed(@TempDir Path dir) {
        ConfigLoader.Loaded loaded = load(List.of("--app.language=  en "), Map.of("TRAJECTORIUM_VIEW_SCALE", " 7 "),
                dir.resolve("none"), Map.of());

        assertEquals("en", loaded.properties().getProperty("app.language"));
        assertEquals("7", scale(loaded));
    }

    @Test
    void userFileWithBrokenEncodingIsReportedAndIgnored(@TempDir Path dir) throws IOException {
        Path user = dir.resolve("config.properties");
        Files.write(user, new byte[]{'v', 'i', 'e', 'w', '.', 's', 'c', 'a', 'l', 'e', '=', (byte) 0xC3, 0x28});

        ConfigLoader.Loaded loaded = load(List.of(), Map.of(), user, Map.of("/config.properties", "view.scale=100"));

        assertEquals("100", scale(loaded));
        assertTrue(loaded.warnings().stream().anyMatch(w -> w.contains("Не удалось прочитать")));
    }

    @Test
    void directoryInsteadOfUserFileIsIgnored(@TempDir Path dir) {
        ConfigLoader.Loaded loaded = load(List.of(), Map.of(), dir, Map.of("/config.properties", "view.scale=100"));
        assertEquals("100", scale(loaded));
    }

    @Test
    void environmentNamesFollowTheRuleAndDoNotCollide() {
        assertEquals("TRAJECTORIUM_VIEW_SCALE", ConfigLoader.envName("view.scale"));
        assertEquals("TRAJECTORIUM_SIMULATION_STEPDAYS", ConfigLoader.envName("simulation.stepDays"));

        assertEquals(AppConfig.KNOWN_KEYS.size(),
                AppConfig.KNOWN_KEYS.stream().map(ConfigLoader::envName).collect(Collectors.toSet()).size());
    }

    @Test
    void knownKeysMatchWhatAppConfigReallyReads() {
        assertEquals(AppConfig.KNOWN_KEYS, new AppConfig(new Properties()).asMap().keySet());
    }
}