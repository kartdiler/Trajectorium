package trajectorium.ui;

import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import trajectorium.*;
import trajectorium.command.*;

import java.util.List;

public class ControlPanel extends VBox {
    private static final String WHITE_TEXT = "-fx-text-fill: white";

    private final Simulation sim;
    private final CommandHistory history;

    private final Button pauseButton = new Button("Пауза");
    private final Button resetButton = new Button("Сброс");
    private final Button undoButton = new Button("Отменить");
    private final Button redoButton = new Button("Повторить");
    private final Slider speedSlider = new Slider(0.1, 20, 1);
    private final Label speedLabel = new Label();
    private final Label timeLabel = new Label();
    private final Label help = new Label();
    private final ComboBox<Integrator> integratorBox = new ComboBox<Integrator>(
            FXCollections.observableArrayList(new VerletIntegrator(), new EulerIntegrator())
    );

    public ControlPanel(Simulation sim, CommandHistory history, Runnable onReset) {
        super(10);
        this.sim = sim;
        this.history = history;

        setPrefWidth(240);
        setStyle("-fx-background-color: #222222; -fx-padding: 12;");
        speedLabel.setStyle(WHITE_TEXT);
        timeLabel.setStyle(WHITE_TEXT);
        help.setStyle(WHITE_TEXT);

        pauseButton.setText(sim.isRunning() ? "Пауза" : "Продолжить");
        speedSlider.setValue(sim.getSimulationSpeed());
        showSpeed(sim.getSimulationSpeed());
        showTime(sim.getTime());
        refreshHistoryButtons();

        pauseButton.setOnAction(e ->
                history.execute(new SetRunningCommand(sim, !sim.isRunning())));
        resetButton.setOnAction(e -> {
                history.execute(new ResetCommand(sim));
                onReset.run();
        });
        undoButton.setOnAction(e -> history.undo());
        redoButton.setOnAction(e -> history.redo());

        speedSlider.valueProperty().addListener((obs, old, now) -> showSpeed(now.doubleValue()));
        speedSlider.setOnMouseReleased(e -> {
            double v = Math.round(speedSlider.getValue() * 10) / 10.0;
            if (v != sim.getSimulationSpeed()) {
                history.execute(new SetSpeedCommand(sim, v));
            }
        });

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
        integratorBox.setOnAction(e -> {
            Integrator chosen = integratorBox.getValue();
            if (chosen != null && !chosen.getName().equals(sim.getIntegrator().getName())) {
                history.execute(new SetIntegratorCommand(sim, chosen));
            }
        });

        help.setText("ЛКМ по телу: следить за ним\nПКМ: добавить тело\nПробел: пауза\nCtrl+Z / Ctrl+Y: отмена / повтор");

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
                pauseButton.setText(running ? "Пауза" : "Продолжить");
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

        for (Control c : List.of(pauseButton, resetButton, undoButton, redoButton, speedSlider, integratorBox)) {
            c.setFocusTraversable(false);
        }

        getChildren().addAll(
                new HBox(8, pauseButton, resetButton),
                new HBox(8, undoButton, redoButton),
                speedLabel, speedSlider, timeLabel, new Label("Метод интегрирования:"), integratorBox,
                help);
    }

    private void showSpeed(double v) {
        speedLabel.setText(String.format("Скорость: x%.1f", v));
    }

    private void showTime(double years) {
        timeLabel.setText(String.format("Время: %.0f дн", years * 365.25));
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
