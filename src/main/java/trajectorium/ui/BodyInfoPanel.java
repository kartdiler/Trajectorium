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
import trajectorium.i18n.I18n;

public class BodyInfoPanel extends VBox {
    private final Simulation sim;
    private final Label title = new Label();
    private final Label mass = new Label();
    private final Label position = new Label();
    private final Label speed = new Label();
    private final Label reference = new Label();
    private final Button deleteButton = new Button();
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

        I18n.addListener(this::refresh);

        getChildren().addAll(title, mass, position, speed, reference, deleteButton);
        refresh();
    }

    public void setBody(Celestial body) {
        this.body = body;
        refresh();
    }

    private void refresh() {
        deleteButton.setText(I18n.get("info.delete"));

        if (body == null) {
            title.setText(I18n.get("info.none"));
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
        mass.setText(I18n.format("info.mass", info.mass(), earthMasses));
        position.setText(I18n.format("info.position", info.x(), info.y()));
        speed.setText(I18n.format("info.speed", info.speedAuYear(), info.speedKmS()));
        reference.setText(info.refName() == null ? "" : I18n.format("info.reference", info.refName(), info.refDistance()));

        deleteButton.setDisable(false);
    }
}
