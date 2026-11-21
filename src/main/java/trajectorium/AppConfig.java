package trajectorium;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AppConfig {
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    public static final Set<String> KNOWN_KEYS = Set.of(
            "canvas.width", "canvas.height", "view.scale", "simulation.stepDays", "simulation.speed",
            "trail.maxPoints", "app.language", "ui.diagnostics", "window.remember"
    );

    private final double canvasWidth;
    private final double canvasHeight;
    private final double scale;
    private final double stepDays;
    private final double simulationSpeed;
    private final int trailMaxPoints;
    private final String language;
    private final boolean diagnostics;
    private final boolean windowRemember;

    public AppConfig(Properties p) {
        canvasWidth = readDouble(p, "canvas.width", 1200, 100, 10000);
        canvasHeight = readDouble(p, "canvas.height", 800, 100, 10000);
        scale = readDouble(p, "view.scale", 100, 1, 100000);
        stepDays = readDouble(p, "simulation.stepDays", 1, 0.001, 365);
        simulationSpeed = readDouble(p, "simulation.speed", 1.0, 0, 100);
        trailMaxPoints = (int) readDouble(p, "trail.maxPoints", 150, 0, 100000);
        language = readLanguage(p, "app.language", "ru");
        diagnostics = readBoolean(p, "ui.diagnostics", false);
        windowRemember = readBoolean(p, "window.remember", true);
    }

    public Map<String, Object> asMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("canvas.width", canvasWidth);
        m.put("canvas.height", canvasHeight);
        m.put("view.scale", scale);
        m.put("simulation.stepDays", stepDays);
        m.put("simulation.speed", simulationSpeed);
        m.put("trail.maxPoints", trailMaxPoints);
        m.put("app.language", language);
        m.put("ui.diagnostics", diagnostics);
        m.put("window.remember", windowRemember);
        return m;
    }

    public static AppConfig load(List<String> args) {
        ConfigLoader.Loaded loaded = ConfigLoader.loadDefault(args);
        loaded.warnings().forEach(w -> log.warn("Конфигурация: {}", w));
        loaded.sources().forEach((key, source) -> log.debug("Конфигурация: {} <- {}", key, source));
        return new AppConfig(loaded.properties());
    }

    public static AppConfig load() {
        return load(List.of());
    }

    private static double readDouble(Properties p, String key, double def, double min, double max) {
        String raw = p.getProperty(key);
        if(raw == null) return def;

        try {
            double v = Double.parseDouble(raw.trim());
            if(Double.isNaN(v) || v < min || v > max) {
                throw new NumberFormatException("вне диапазона");
            }

            return v;
        } catch (NumberFormatException ex) {
            log.warn("Неверное значение {} = '{}', используется {}", key, raw, def);
            return def;
        }
    }

    private static String readLanguage(Properties p, String key, String def) {
        String raw = p.getProperty(key);
        if (raw == null) {
            return def;
        }
        String v = raw.trim().toLowerCase();
        if (v.equals("ru") || v.equals("en")) {
            return v;
        }
        log.warn("Неверное значение {}='{}', используется {}", key, raw, def);
        return def;
    }

    private static boolean readBoolean(Properties p, String key, boolean def) {
        String raw = p.getProperty(key);
        if (raw == null) {
            return def;
        }
        String v = raw.trim().toLowerCase();
        if (v.equals("true")) {
            return true;
        }
        if (v.equals("false")) {
            return false;
        }
        log.warn("Неверное значение {}='{}', используется {}", key, raw, def);
        return def;
    }

    public double getCanvasWidth() {
        return canvasWidth;
    }

    public double getCanvasHeight() {
        return canvasHeight;
    }

    public double getScale() {
        return scale;
    }

    public double getStepDays() {
        return stepDays;
    }

    public double getDt() {
        return stepDays / 365.25;
    }

    public double getSimulationSpeed() {
        return simulationSpeed;
    }

    public int getTrailMaxPoints() {
        return trailMaxPoints;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isDiagnostics() {
        return diagnostics;
    }

    public boolean isWindowRemember() {
        return windowRemember;
    }
}
