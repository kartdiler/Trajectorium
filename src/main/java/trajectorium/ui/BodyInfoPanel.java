package trajectorium.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import trajectorium.Celestial;
import trajectorium.CelestialFactory.MassUnit;
import trajectorium.Simulation;
import trajectorium.SimulationListener;
import trajectorium.command.CommandHistory;
import trajectorium.command.RemoveBodyCommand;

public class BodyInfoPanel extends VBox {
    private final Simulation sim;
    private final Label title = new Label();
    private final Label mass = new Label();
    private final Label position = new Label();
    private final Label speed = new Label();
    private final Label reference = new Label();
    private final Button deleteButton = new Button("Удалить тело");
    private Celestial body;

    public BodyInfoPanel(Simulation sim, CommandHistory history) {
        super(6);
        this.sim = sim;

        setPrefHeight(240);
        setStyle("-fx-padding: 12;");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        for (Label l : new Label[] {mass, position, speed, reference}) {
            l.setStyle("-fx-text-fill: white");
        }

        deleteButton.setFocusTraversable(false);
        deleteButton.setOnAction(e -> {
            if (body != null) {
                history.execute(new RemoveBodyCommand(sim, body));
            }
        });

        sim.addListener(new SimulationListener() {
            @Override
            public void onStep(Simulation sim) {
                refresh();
            }
        });

        getChildren().addAll(title, mass, position, speed, reference, deleteButton);
        refresh();
    }

    public void setBody(Celestial body) {
        this.body = body;
        refresh();
    }

    private void refresh() {
        if (body == null) {
            title.setText("Тело не выбрано");
            mass.setText("");
            speed.setText("");
            position.setText("");
            reference.setText("");
            deleteButton.setDisable(true);
            return;
        }

        BodyInfo info = BodyInfo.of(body, sim);
        double earthMasses = info.mass() / MassUnit.EARTH_MASSES.toSolarMasses(1.0);

        title.setText(info.name());
        mass.setText(String.format("Масса: %.3e M Солнц (%.3g M Земли)", info.mass(), earthMasses));
        position.setText(String.format("Позиция: %.3f; %.3f а.е.", info.x(), info.y()));
        speed.setText(String.format("Скорость: %.3f а.е./год (%.2f км/с)", info.speedAuYear(), info.speedKmS()));
        reference.setText(info.refName() == null ? "" : String.format("До %s: %.3f а.е.", info.refName(), info.refDistance()));

        deleteButton.setDisable(false);
    }
}
