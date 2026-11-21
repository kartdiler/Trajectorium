package trajectorium;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class UserDirsTest {

    @Test
    void defaultsToTheUserHomeFolder() {
        Properties system = new Properties();
        system.setProperty("user.home", "/home/anna");

        UserDirs dirs = UserDirs.resolve(Map.of(), system);

        assertEquals(Path.of("/home/anna", ".trajectorium"), dirs.base());
        assertEquals(dirs.base().resolve("config.properties"), dirs.configFile());
        assertEquals(dirs.base().resolve("window.json"), dirs.windowFile());
    }

    @Test
    void systemPropertyBeatsEnvironmentVariable() {
        Properties system = new Properties();
        system.setProperty("user.home", "/home/anna");
        system.setProperty("trajectorium.home", "/opt/a");

        assertEquals(Path.of("/opt/a"), UserDirs.resolve(Map.of("TRAJECTORIUM_HOME", "/opt/b"), system).base());
        assertEquals(Path.of("/opt/b"),
                UserDirs.resolve(Map.of("TRAJECTORIUM_HOME", " /opt/b "), new Properties()).base());
    }
}