package trajectorium;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public record UserDirs(Path base) {
    public static UserDirs resolve(Map<String, String> env, Properties system) {
        String override = system.getProperty("trajectorium.home");
        if (override == null || override.isBlank()) {
            override = env.get("TRAJECTORIUM_HOME");
        }

        Path base = (override != null && !override.isBlank())
                ? Path.of(override.trim())
                : Path.of(system.getProperty("user.home", "."), ".trajectorium");
        return new UserDirs(base);
    }

    public Path configFile() {
        return base.resolve("config.properties");
    }

    public Path windowFile() {
        return base.resolve("window.json");
    }
}
