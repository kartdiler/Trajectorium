package trajectorium.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import trajectorium.*;
import trajectorium.command.*;
import trajectorium.i18n.I18n;

import java.util.List;

public class ControlPanel extends VBox {
    private static final String WHITE_TEXT = "-fx-text-fill: white";

    private final Simulation sim;
    private final CommandHistory history;

    private final Button pauseButton = new Button();
    private final Button resetButton = new Button();
    private final Button undoButton = new Button();
    private final Button redoButton = new Button();
    private final Button saveButton = new Button();
    private final Button openButton = new Button();
    private final Slider speedSlider = new Slider(0.1, 20, 1);
    private final Label speedLabel = new Label();
    private final Label timeLabel = new Label();
    private final Label help = new Label();
    private final Label integratorLabel = new Label();
    private final ComboBox<Integrator> integratorBox = new ComboBox<Integrator>(
            FXCollections.observableArrayList(new VerletIntegrator(), new EulerIntegrator())
    );
    private final ComboBox<String> presetBox = new ComboBox<String>();
    private final Label languageLabel = new Label();
    private final ComboBox<String> languageBox = new ComboBox<String>(FXCollections
            .observableArrayList("Русский", "English"));

    public ControlPanel(Simulation sim, CommandHistory history, Runnable onReset, SceneFileController files) {
        super(10);
        this.sim = sim;
        this.history = history;

        setPrefWidth(240);
        setStyle("-fx-background-color: #222222; -fx-padding: 12;");
        for (Label l : List.of(speedLabel, timeLabel, integratorLabel, languageLabel)) {
            l.setStyle(WHITE_TEXT);
        }

        help.setStyle("-fx-text-fill: #aaaaaa;");
        languageLabel.setText(I18n.get("control.language"));

        speedSlider.setValue(sim.getSimulationSpeed());

        integratorBox.setConverter(new StringConverter<Integrator>() {
            @Override
            public String toString(Integrator integrator) {
                return integrator == null ? "" : integrator.getName();
            }

            @Override
            public Integrator fromString(String s) {
                return null;
            }
        });
        selectIntegrator(sim.getIntegrator());

        languageBox.getSelectionModel().select(I18n.getLocale().getLanguage().equals("en") ? 1 : 0);

        presetBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? I18n.get("control.preset.prompt") : item);
            }
        });

        pauseButton.setOnAction(e ->
                history.execute(new SetRunningCommand(sim, !sim.isRunning())));
        resetButton.setOnAction(e -> {
            history.execute(new ResetCommand(sim));
            onReset.run();
        });
        undoButton.setOnAction(e -> history.undo());
        redoButton.setOnAction(e -> history.redo());
        saveButton.setOnAction(e -> files.save(getScene().getWindow()));
        openButton.setOnAction(e -> files.load(getScene().getWindow()));

        speedSlider.valueProperty().addListener((obs, old, now) -> showSpeed(now.doubleValue()));
        speedSlider.setOnMouseReleased(e -> {
            double v = Math.round(speedSlider.getValue() * 10) / 10.0;
            if (v != sim.getSimulationSpeed()) {
                history.execute(new SetSpeedCommand(sim, v));
            }
        });

        integratorBox.setOnAction(e -> {
            Integrator chosen = integratorBox.getValue();
            if (chosen != null && !chosen.getName().equals(sim.getIntegrator().getName())) {
                history.execute(new SetIntegratorCommand(sim, chosen));
            }
        });

        presetBox.setOnAction(e -> {
            int index = presetBox.getSelectionModel().getSelectedIndex();
            if (index < 0) return;
            files.loadPreset(index);
            Platform.runLater(() -> presetBox.getSelectionModel().clearSelection());
        });

        languageBox.setOnAction(e -> {
            I18n.setLocale(languageBox.getSelectionModel().getSelectedIndex() == 1 ? I18n.EN : I18n.RU);
        });

        history.setOnChange(this::refreshHistoryButtons);

        sim.addListener(new SimulationListener() {
            @Override
            public void onStep(Simulation sim) {
                showTime(sim.getTime());
            }

            @Override
            public void onReset() {
                showTime(0);
            }

            @Override
            public void onRunningChanged(boolean running) {
                pauseButton.setText(I18n.get(running ? "control.pause" : "control.resume"));
            }

            @Override
            public void onSpeedChanged(double speed) {
                speedSlider.setValue(speed);
                showSpeed(speed);
            }

            @Override
            public void onIntegratorChanged(Integrator i) {
                selectIntegrator(i);
            }
        });

        I18n.addListener(this::updateTexts);

        for (Control c : List.of(pauseButton, resetButton, undoButton, redoButton, speedSlider, integratorBox,
                saveButton, openButton, presetBox, languageBox)) {
            c.setFocusTraversable(false);
        }

        getChildren().addAll(
                new HBox(8, pauseButton, resetButton),
                new HBox(8, undoButton, redoButton),
                new HBox(8, saveButton, openButton),
                presetBox,
                speedLabel, speedSlider, timeLabel,
                integratorLabel, integratorBox,
                help,
                languageLabel, languageBox);

        updateTexts();
        refreshHistoryButtons();
    }

    private void updateTexts() {
        resetButton.setText(I18n.get("control.reset"));
        undoButton.setText(I18n.get("control.undo"));
        redoButton.setText(I18n.get("control.redo"));
        saveButton.setText(I18n.get("control.save"));
        openButton.setText(I18n.get("control.open"));
        pauseButton.setText(I18n.get(sim.isRunning() ? "control.pause" : "control.resume"));
        integratorLabel.setText(I18n.get("control.integrator"));
        help.setText(I18n.get("control.help"));
        presetBox.setPromptText(I18n.get("control.preset.prompt"));
        presetBox.getItems().setAll(SceneFileController.presetTitles());
        showSpeed(sim.getSimulationSpeed());
        showTime(sim.getTime());
    }

    private void showSpeed(double v) {
        speedLabel.setText(I18n.format("control.speed", v));
    }

    private void showTime(double years) {
        timeLabel.setText(I18n.format("control.time", years * 365.25));
    }

    private void refreshHistoryButtons() {
        undoButton.setDisable(!history.canUndo());
        redoButton.setDisable(!history.canRedo());
    }

    private void selectIntegrator(Integrator current) {
        for (Integrator item : integratorBox.getItems()) {
            if (item.getName().equals(current.getName())) {
                integratorBox.setValue(item);
                return;
            }
        }
    }
}
