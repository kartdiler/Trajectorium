package trajectorium.ui;

import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trajectorium.CelestialFactory;
import trajectorium.Simulation;
import trajectorium.command.CommandHistory;
import trajectorium.command.LoadSceneCommand;
import trajectorium.i18n.I18n;
import trajectorium.io.SceneData;
import trajectorium.io.SceneFormatException;
import trajectorium.io.SceneMapper;
import trajectorium.io.SceneStorage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class SceneFileController {
    public record Preset(String titleKey, String resource) {}

    public static final List<Preset> PRESETS = List.of(
            new Preset("preset.solar", "/scenes/solar-system.json"),
            new Preset("preset.binary", "/scenes/binary-star.json")
    );

    public static List<String> presetTitles() {
        return PRESETS.stream().map(p -> I18n.get(p.titleKey())).toList();
    }

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
            FileChooser chooser = chooser(I18n.get("file.save.title"));
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
                fail(I18n.get("file.error.save"), e);
            }
        });
    }

    public void load(Window owner) {
        runPaused(() -> {
            File file = chooser(I18n.get("file.open.title")).showOpenDialog(owner);
            if (file == null) {
                return;
            }
            lastDirectory = file.getParentFile();

            try {
                SceneData data = SceneStorage.load(file.toPath());
                history.execute(new LoadSceneCommand(sim, factory, data));
                log.info("Сцена загружена: {}", file);
            } catch (IOException | SceneFormatException e) {
                fail(I18n.get("file.error.open"), e);
            }
        });
    }

    public void loadPreset(int index) {
        if (index < 0 || index >= PRESETS.size()) return;
        Preset p = PRESETS.get(index);
        String title = I18n.get(p.titleKey());
        try {
            history.execute(new LoadSceneCommand(sim, factory, SceneStorage.loadResource(p.resource())));
            log.info("Загружен пресет: {}", title);
        } catch (IOException | SceneFormatException e) {
            fail(I18n.format("file.error.preset", title), e);
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
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.get("file.filter"), "*.json"));
        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }

        return chooser;
    }

    private void fail(String header, Exception e) {
        log.error(header, e);
        Alert alert = new Alert(Alert.AlertType.ERROR, "",
                new ButtonType(I18n.get("dialog.ok"), ButtonBar.ButtonData.OK_DONE));
        alert.setTitle("Trajectorium");
        alert.setHeaderText(header);

        String message = e.getMessage() == null ? e.toString() : e.getMessage();
        Label text = new Label(message);
        text.setWrapText(true);
        text.setMaxWidth(460);
        alert.getDialogPane().setContent(text);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        StringWriter trace = new StringWriter();
        e.printStackTrace(new PrintWriter(trace));
        TextArea details = new TextArea(trace.toString());
        details.setEditable(false);
        details.setPrefRowCount(10);
        alert.getDialogPane().setExpandableContent(details);

        alert.showAndWait();
    }
}
