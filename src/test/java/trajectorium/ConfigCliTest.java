package trajectorium;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigCliTest {
    private static String text(ByteArrayOutputStream out) {
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    void printShowsValuesWithTheirSources() {
        Properties props = new Properties();
        props.setProperty("view.scale", "200");
        ConfigLoader.Loaded loaded = new ConfigLoader.Loaded(props, Map.of("view.scale", "аргумент --view.scale"),
                List.of("Неизвестный параметр «x»"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int code = ConfigCli.print(loaded, new PrintStream(out, true, StandardCharsets.UTF_8));

        assertEquals(0, code);
        String result = text(out);
        assertTrue(result.contains("view.scale") && result.contains("200.0") && result.contains("[аргумент --view.scale]"), result);
        assertTrue(result.contains("[по умолчанию]"), result);        // ключи без источника
        assertTrue(result.contains("! Неизвестный параметр «x»"), result);
    }

    @Test
    void initCreatesATemplateOnceAndTheTemplateChangesNothing(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("sub").resolve("config.properties");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(out, true, StandardCharsets.UTF_8);

        assertEquals(0, ConfigCli.init(file, stream));
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("#view.scale=100.0"));

        assertEquals(1, ConfigCli.init(file, stream));

        ConfigLoader.Loaded loaded = ConfigLoader.load(List.of(), Map.of(), file, path -> null);
        assertTrue(loaded.properties().isEmpty());
        assertTrue(loaded.warnings().stream().noneMatch(w -> w.contains("Не удалось")));
    }
}