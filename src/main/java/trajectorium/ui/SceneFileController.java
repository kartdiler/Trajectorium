package trajectorium.ui;

import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trajectorium.CelestialFactory;
import trajectorium.Simulation;
import trajectorium.command.CommandHistory;
import trajectorium.command.LoadSceneCommand;
import trajectorium.io.SceneData;
import trajectorium.io.SceneFormatException;
import trajectorium.io.SceneMapper;
import trajectorium.io.SceneStorage;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SceneFileController {
    public record Preset(String title, String resource) {}

    public static final List<Preset> PRESETS = List.of(
            new Preset("Солнечная система", "/scenes/solar-system.json"),
            new Preset("Двойная звезда", "/scenes/binary-star.json")
    );

    private static final Logger log = LoggerFactory.getLogger(SceneFileController.class);

    private final Simulation sim;
    private final CelestialFactory factory;
    private final CommandHistory history;
    private File lastDirectory;

    public SceneFileController(Simulation sim, CelestialFactory factory, CommandHistory history) {
        this.sim = sim;
        this.factory = factory;
        this.history = history;
    }

    public void save(Window owner) {
        runPaused(() -> {
            FileChooser chooser = chooser("Сохранить сцену");
            chooser.setInitialFileName("scene.json");
            File file = chooser.showSaveDialog(owner);
            if (file == null) return;
            if (!file.getName().toLowerCase().endsWith(".json")) {
                file = new File(file.getParentFile(), file.getName() + ".json");
            }
            lastDirectory = file.getParentFile();
            try {
                SceneStorage.save(file.toPath(), SceneMapper.capture(sim));
                log.info("Сцена сохранена: {}", file);
            } catch (IOException | SceneFormatException e) {
                fail("Не удалось сохранить сцену", e);
            }
        });
    }

    public void load(Window owner) {
        runPaused(() -> {
            File file = chooser("Открыть сцену").showOpenDialog(owner);
            if (file == null) {
                return;
            }
            lastDirectory = file.getParentFile();

            try {
                SceneData data = SceneStorage.load(file.toPath());
                history.execute(new LoadSceneCommand(sim, factory, data));
                log.info("Сцена загружена: {}", file);
            } catch (IOException | SceneFormatException e) {
                fail("Не удалось открыть сцену", e);
            }
        });
    }

    public void loadPreset(String title) {
        for (Preset p : PRESETS) {
            if (!p.title().equals(title)) continue;
            try {
                history.execute(new LoadSceneCommand(sim, factory, SceneStorage.loadResource(p.resource())));
                log.info("Загружен пресет: {}", title);
            } catch (IOException | SceneFormatException e) {
                fail("Не удалось загрузить пресет \"" + title + "\"", e);
            }
            return;
        }
    }

    private void runPaused(Runnable action) {
        boolean wasRunning = sim.isRunning();
        if (wasRunning) {
            sim.pause();
        }

        try {
            action.run();
        } finally {
            if (wasRunning) {
                sim.play();
            }
        }
    }

    private FileChooser chooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Сцены Trajectorium (*.json)", "*.json"));
        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }

        return chooser;
    }

    private void fail(String header, Exception e) {
        log.error(header, e);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Trajectorium");
        alert.setHeaderText(header);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }
}
