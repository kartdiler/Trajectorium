package trajectorium.report;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class PngEncoder {
    private PngEncoder() {}

    public static byte[] encode(int[] argb, int width, int height) throws IOException {
        if (argb == null || width <= 0 || height <= 0 || argb.length < (long) width * height) {
            throw new IllegalArgumentException("Pixel array does not match " + width + "x" + height);
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, argb, 0, width);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", out)) {
            throw new IOException("PNG writer is not available");
        }
        return out.toByteArray();
    }
}
