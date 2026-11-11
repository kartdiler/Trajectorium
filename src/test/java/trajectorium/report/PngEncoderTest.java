package trajectorium.report;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PngEncoderTest {
    @Test
    void encodesPixelsIntoReadablePng() throws IOException {
        int[] pixels = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF};

        byte[] png = PngEncoder.encode(pixels, 2, 2);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(2, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(0xFFFF0000, image.getRGB(0, 0));
        assertEquals(0xFF00FF00, image.getRGB(1, 0));
        assertEquals(0xFF0000FF, image.getRGB(0, 1));
        assertEquals(0xFFFFFFFF, image.getRGB(1, 1));
    }

    @Test
    void rejectsPixelArraysThatDoNotMatchTheSize() {
        assertThrows(IllegalArgumentException.class, () -> PngEncoder.encode(new int[3], 2, 2));
        assertThrows(IllegalArgumentException.class, () -> PngEncoder.encode(new int[4], 0, 4));
        assertThrows(IllegalArgumentException.class, () -> PngEncoder.encode(null, 2, 2));
    }

    @Test
    void encodedImageCanBeEmbeddedIntoWordReport() throws IOException {
        int width = 120;
        int height = 80;
        int[] pixels = new int[width * height];
        java.util.Arrays.fill(pixels, 0xFF202020);

        byte[] png = PngEncoder.encode(pixels, width, height);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new WordReportRenderer(png).render(ReportFixtures.sampleReport(), out);
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(out.toByteArray()))) {
            assertEquals(1, doc.getAllPictures().size());
        }
    }
}
