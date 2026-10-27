package trajectorium.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.util.StringConverter;
import trajectorium.CelestialFactory.MassUnit;

import java.util.List;
import java.util.Optional;

public final class NewBodyDialog {
    private NewBodyDialog() {}

    public record Result(String name, double mass, MassUnit unit,
                         double vx, double vy, int radius, Color color) {}

    public static Optional<Result> show(Window owner, double x, double y, String defaultName,
                                        String speedHint) {
        Dialog<Result> dialog = new Dialog<Result>();
        dialog.initOwner(owner);
        dialog.setTitle("Новое тело");
        dialog.setHeaderText(String.format("Позиция: x = %.3f, y = %.3f а.е.", x, y));

        ButtonType addType = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        TextField name = new TextField(defaultName);
        TextField mass = new TextField("1");
        ComboBox<MassUnit> unit = new ComboBox<MassUnit>(FXCollections.observableArrayList(MassUnit.values()));
        unit.setValue(MassUnit.EARTH_MASSES);
        unit.setConverter(new StringConverter<MassUnit>() {
            @Override
            public String toString(MassUnit massUnit) {
                return unitLabel(massUnit);
            }

            @Override
            public MassUnit fromString(String s) {
                return null;
            }
        });

        TextField vx = new TextField("0");
        TextField vy = new TextField("0");
        Spinner<Integer> radius = new Spinner<Integer>(1, 50, 4);
        ColorPicker color = new ColorPicker(Color.LIGHTGREEN);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        grid.addRow(0, new Label("Имя"), name);
        grid.addRow(1, new Label("Масса"), mass, unit);
        grid.addRow(2, new Label("Скорость по x, а.е./год"), vx);
        grid.addRow(3, new Label("Скорость по y, а.е./год"), vy);
        grid.addRow(4, new Label("Радиус на экране, px"), radius);
        grid.addRow(5, new Label("Цвет"), color);

        if (!speedHint.isEmpty()) {
            grid.add(new Label(speedHint), 0, 6, 3, 1);
        }

        dialog.getDialogPane().setContent(grid);

        Node addButton = dialog.getDialogPane().lookupButton(addType);
        Runnable validate = () -> {
            boolean nameOk = InputParser.isValidName(name.getText());
            boolean massOk = InputParser.parsePositive(mass.getText()).isPresent();
            boolean vxOk = InputParser.parseNumber(vx.getText()).isPresent();
            boolean vyOk = InputParser.parseNumber(vy.getText()).isPresent();
            mark(name, nameOk);
            mark(mass, massOk);
            mark(vx, vxOk);
            mark(vy, vyOk);
            addButton.setDisable(!(nameOk && massOk && vxOk && vyOk));
        };

        for (TextField f : List.of(name, mass, vx, vy)) {
            f.textProperty().addListener((obs, old, now) -> validate.run());
        }
        validate.run();

        dialog.setResultConverter(button -> {
            if (button != addType){
                return null;
            }

            return new Result(
                    name.getText().trim(),
                    InputParser.parsePositive(mass.getText()).getAsDouble(),
                    unit.getValue(),
                    InputParser.parseNumber(vx.getText()).getAsDouble(),
                    InputParser.parseNumber(vy.getText()).getAsDouble(),
                    radius.getValue(),
                    color.getValue()
            );
        });

        return dialog.showAndWait();
    }

    private static void mark(TextField field, boolean ok) {
        field.setStyle(ok ? "" : "-fx-border-color: red;");
    }

    private static String unitLabel(MassUnit u) {
        return switch (u) {
            case SOLAR_MASSES -> "М Солнца";
            case EARTH_MASSES -> "М Земли";
            case KILOGRAMS -> "кг";
        };
    }
}
