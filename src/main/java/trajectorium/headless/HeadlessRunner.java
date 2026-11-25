package trajectorium.headless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trajectorium.AppConfig;
import trajectorium.CelestialFactory;
import trajectorium.Simulation;
import trajectorium.io.SceneData;
import trajectorium.io.SceneFormatException;
import trajectorium.io.SceneMapper;
import trajectorium.io.SceneStorage;
import trajectorium.report.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public final class HeadlessRunner {
    private HeadlessRunner() {}

    public static final int OK = 0;
    public static final int USAGE = 2;
    public static final int SCENE_ERROR = 3;
    public static final int OUTPUT_ERROR = 4;

    private static final Logger log = LoggerFactory.getLogger(HeadlessRunner.class);

    public static int run(HeadlessOptions options, AppConfig config, PrintStream out, PrintStream err) {
        SceneData data;
        try {
            data = loadScene(options.scene());
        } catch (IOException | SceneFormatException e) {
            log.error("Сцена не загружена: {}", options.scene(), e);
            err.println("Сцена не загружена: " + e.getMessage());
            return SCENE_ERROR;
        }

        Simulation sim = new Simulation(config.getDt());
        CelestialFactory factory = new CelestialFactory(sim, config.getScale());
        factory.setTrailMaxPoints(0);
        SceneMapper.apply(data, sim, factory);
        sim.setSimulationSpeed(1.0);

        long steps = stepsFor(options, sim.getDt());
        if (steps > HeadlessOptions.MAX_STEPS) {
            err.println("Слишком много шагов: " + steps + " (максимум " + HeadlessOptions.MAX_STEPS + "). Увеличьте simulation.stepDays или уменьшите --years.");
            return USAGE;
        }

        log.info("Безголовый запуск: сцена {}, тел {}, шагов {}, метод {}",
                options.scene(), sim.getBodies().size(), steps, sim.getIntegrator().getName());
        long started = System.nanoTime();
        log.info("Расчет завершен: {} шагов за {} мс", steps, (System.nanoTime() - started) / 1_000_000);

        try {
            byte[] result = render(options.format(), sim);
            write(options.out(), result, out);
            log.info("Результат записан: {} ({} байт)", options.out(), result.length);
        } catch (IOException | SceneFormatException | RuntimeException e) {
            log.error("Результат не записан", e);
            err.println("Результат не записан: " + e.getMessage());
            return OUTPUT_ERROR;
        }
        return OK;
    }

    public static long stepsFor(HeadlessOptions options, double dtYears) {
        return options.steps() != null ? options.steps() : Math.max(1, Math.round(options.years() / dtYears));
    }

    static SceneData loadScene(String spec) throws IOException, SceneFormatException {
        if (spec.startsWith("preset:")) {
            return SceneStorage.loadResource("/scenes/" + spec.substring("preset:".length()) + ".json");
        }
        return SceneStorage.load(Path.of(spec));
    }

    private static byte[] render(HeadlessOptions.Format format, Simulation sim) throws IOException, SceneFormatException {
        if (format == HeadlessOptions.Format.SCENE) {
            return SceneStorage.toBytes(SceneMapper.capture(sim));
        }
        ReportRenderer renderer = switch (format) {
            case TXT -> new TextReportRenderer();
            case CSV -> new CsvReportRenderer();
            case XLSX -> new ExcelReportRenderer();
            case DOCX -> new WordReportRenderer();
            case SCENE -> throw new IllegalStateException();
        };
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        renderer.render(ReportBuilder.build(sim, LocalDateTime.now()), buffer);
        return buffer.toByteArray();
    }

    private static void write(String target, byte[] bytes, PrintStream console) throws IOException {
        if (target.equals("-")) {
            console.write(bytes, 0, bytes.length);
            console.flush();
        } else {
            Path file = Path.of(target);
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.write(file, bytes);
        }
    }
}
