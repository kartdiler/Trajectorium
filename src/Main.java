import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.Scene;

public class Main extends Application {
    static void main() {
        launch();
    }

    final double GM = 4 * Math.PI * Math.PI;
    double x = 1.0, y = 0;
    double vx = 0 * Math.PI, vy = 2 * Math.PI;
    double dt = 1.0 / 365.25;

    double canvasWidth = 1200;
    double canvasHeight = 800;

    double sunX = canvasWidth / 2 - 20;
    double sunY = canvasHeight / 2 - 50;
    double earthX;
    double earthY;

    double a0[] = acceleration(x, y);

    double xPrev = x - vx * dt + a0[0] * dt * dt / 2;
    double yPrev = y - vy * dt + a0[1] * dt * dt / 2;

    double[] acceleration(double x, double y) {
        double r = Math.sqrt(x * x + y * y);
        double factor = -GM / Math.pow(r, 3);
        return new double[]{factor * x, factor * y};
    }

    @Override
    public void start(Stage stage) {
        Group root = new Group();
        Canvas canvas = new Canvas(canvasWidth, canvasHeight);
        GraphicsContext gc = canvas.getGraphicsContext2D();
//        gc.setFill(Color.BLACK);
//        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        root.getChildren().add(canvas);

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                gc.setFill(Color.YELLOW);
                gc.fillOval(sunX, sunY, 40, 40);

                double a[] = acceleration(x, y);

                double xNext = 2 * x - xPrev + a[0] * dt * dt;
                double yNext = 2 * y - yPrev + a[1] * dt * dt;

                xPrev = x;
                yPrev = y;
                x = xNext;
                y = yNext;

                earthX = sunX + 100 * x + 10;
                earthY = sunY + 100 * y + 10;

                gc.setFill(Color.DARKCYAN);
                gc.fillOval(earthX, earthY, 10, 10);
                System.out.printf("x=%.4f, y=%.4f\n", earthX, earthY);
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
}