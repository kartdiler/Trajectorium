package trajectorium.report;

import javafx.scene.paint.Color;
import trajectorium.Celestial;
import trajectorium.CelestialFactory;
import trajectorium.Simulation;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

final class ReportFixtures {
    static final double DT = 1.0 / 365.25;
    static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 8, 12, 30);

    private ReportFixtures() {}

    static ReportData sampleReport() {
        Simulation sim = new Simulation(DT);
        CelestialFactory factory = new CelestialFactory(sim, 100);
        Celestial sun = factory.createStar("Sun", 1.0, 0, 0, 20, Color.YELLOW);
        factory.createOrbiting("Earth", 3.003e-6, sun, 1.0, 5, Color.BLUE, true);
        factory.createOrbiting("Jupiter", 9.55e-4, sun, 5.2, 6, Color.ORANGERED, true);
        return ReportBuilder.build(sim, NOW);
    }

    static ReportData syntheticReport(int n) {
        List<ReportData.BodyRow> rows = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            rows.add(new ReportData.BodyRow("B" + i, MassClass.SMALL, 1e-12, i, -i,
                    0.1, 0.2, 0.22, 1.05, 2, "#808080"));
        }
        ReportData.Summary summary = new ReportData.Summary(n, n * 1e-12, "B0", 1.0 / n,
                0.22, 0.22, 0.22, 0, 0, 0, 0, null);
        return new ReportData(NOW, "Verlet", 0, rows, summary, List.of());
    }

    static byte[] pngImage() throws IOException {
        BufferedImage image = new BufferedImage(800, 500, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(java.awt.Color.BLACK);
        g.fillRect(0, 0, 800, 500);
        g.setColor(java.awt.Color.YELLOW);
        g.fillOval(380, 230, 40, 40);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}