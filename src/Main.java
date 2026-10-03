import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {
    static void main() {
        launch();
    }

    Simulation simulation = new Simulation(1.0 / 365.25);

    double canvasWidth = 1200;
    double canvasHeight = 800;
    double scale = 100;

//    final int MAX_TRAIL_POINTS = 150;
//    ArrayDeque<double[]> trail = new ArrayDeque<double[]>(MAX_TRAIL_POINTS);

    @Override
    public void start(Stage stage) {
        Group root = new Group();
        Canvas canvas = new Canvas(canvasWidth, canvasHeight);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        root.getChildren().add(canvas);

        Celestial Sun = new Celestial("Sun", 1, 0, 0,
                0, 0, 20, Color.YELLOW, simulation.getBodies(), simulation.getDt());
        simulation.addBody(Sun);

        Celestial Earth = new Celestial("Earth", 3.003e-6, 1, 0,
                0, circularVelocity(Sun.getMass(), 1), 5, Color.BLUE,
                simulation.getBodies(), simulation.getDt());
        simulation.addBody(Earth);

        double origX = canvasWidth / 2;
        double origY = canvasHeight / 2;

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

                simulation.step();

                for (Celestial cel : simulation.getBodies()) {
                    gc.setFill(cel.getColor());
                    double x = cel.screenX(origX, scale) - cel.getRadius();
                    double y = cel.screenY(origY, scale) - cel.getRadius();
                    gc.fillOval(x, y, cel.getRadius() * 2, cel.getRadius() * 2);
                }
//                trail.add(new double[]{earthX, earthY});
//
//                if (trail.size() > MAX_TRAIL_POINTS) {
//                    trail.removeFirst();
//                }
//
//                gc.setFill(Color.WHITE);
//                for(var point: trail) {
//                    gc.fillOval(point[0] + 5, point[1] + 5, 2, 2);
//                }

            }
        };

        timer.start();

        Scene scene = new Scene(root, Color.BLACK);
        stage.setScene(scene);
        stage.setTitle("Trajectorium");
        stage.setWidth(canvasWidth);
        stage.setHeight(canvasHeight);
        stage.show();
    }

    double circularVelocity(double parentMass, double r) {
        double GM = 4 * Math.PI * Math.PI * parentMass;
        return Math.sqrt(GM / r);
    }
}