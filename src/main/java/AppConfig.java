package main.java;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class AppConfig {
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static final String RESOURCE = "/config.properties";

    private final double canvasWidth;
    private final double canvasHeight;
    private final double scale;
    private final double stepDays;
    private final double simulationSpeed;
    private final int trailMaxPoints;

    public AppConfig(Properties p) {
        canvasWidth = readDouble(p, "canvas.width", 1200, 100, 10000);
        canvasHeight = readDouble(p, "canvas.height", 800, 100, 10000);
        scale = readDouble(p, "view.scale", 100, 1, 100000);
        stepDays = readDouble(p, "simulation.stepDays", 1, 0.001, 365);
        simulationSpeed = readDouble(p, "simulation.speed", 1.0, 0, 100);
        trailMaxPoints = (int) readDouble(p, "trail.maxPoints", 150, 0, 100000);
    }

    public static AppConfig load() {
        Properties p = new Properties();
        try(InputStream in = AppConfig.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                log.warn("config.properties не найден, используются значения по умолчанию");
            } else {
                p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                log.info("Конфигурация загружена из config.properties");
            }
        } catch (IOException ex) {
            log.warn("Не удалось прочитать config.properties", ex);
        }

        return new AppConfig(p);
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
}
