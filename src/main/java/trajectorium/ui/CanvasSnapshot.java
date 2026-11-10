package trajectorium.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trajectorium.report.PngEncoder;

import java.io.IOException;

public final class CanvasSnapshot {
    private static final Logger log = LoggerFactory.getLogger(CanvasSnapshot.class);

    private CanvasSnapshot() {}

    public static byte[] png(Canvas canvas) {
        try {
            if (canvas.getWidth() < 1 || canvas.getHeight() < 1) {
                return null;
            }

            WritableImage image = canvas.snapshot(null, null);
            int width = (int) image.getWidth();
            int height = (int) image.getHeight();
            int[] pixels = new int[width * height];
            image.getPixelReader().getPixels(0, 0, width, height,
                    PixelFormat.getIntArgbInstance(), pixels, 0, width);
            return PngEncoder.encode(pixels, width, height);
        } catch (IOException | RuntimeException e) {
            log.warn("Не удалось сделать снимок сцены, отчет будет без картинки", e);
            return null;
        }
    }
}
