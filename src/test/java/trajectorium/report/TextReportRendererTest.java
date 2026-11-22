package trajectorium.report;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trajectorium.Simulation;
import trajectorium.i18n.I18n;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TextReportRendererTest {

    @BeforeEach
    void useRussian() { I18n.setLocale(I18n.RU); }

    @AfterEach
    void restoreRussian() { I18n.setLocale(I18n.RU); }

    @Test
    void reportContainsTitleTablesAndValues() {
        List<String> lines = TextReportRenderer.lines(ReportFixtures.sampleReport());
        String text = String.join("\n", lines);

        assertEquals(I18n.get("report.title"), lines.get(0));
        assertTrue(text.contains(I18n.get("report.word.h.bodies")));
        assertTrue(text.contains(I18n.get("report.word.h.summary")));
        assertTrue(text.contains(I18n.get("report.classes.title")));
        assertTrue(text.contains("| Earth"), text);
        assertTrue(text.contains("3,003e-06"), text);
        assertTrue(text.contains(I18n.get("class.giant")));
    }

    @Test
    void textFollowsTheLanguage() {
        I18n.setLocale(I18n.EN);
        String text = String.join("\n", TextReportRenderer.lines(ReportFixtures.sampleReport()));

        assertTrue(text.contains("Trajectorium simulation report"));
        assertTrue(text.contains("3.003e-06"), text);
    }

    @Test
    void tablesFitIntoAPortraitPage() {
        int longest = TextReportRenderer.lines(ReportFixtures.sampleReport()).stream()
                .mapToInt(String::length).max().orElseThrow();

        assertTrue(longest <= 110, "самая длинная строка: " + longest);
    }

    @Test
    void emptySceneGivesTitleAndMessageOnly() {
        List<String> lines = TextReportRenderer.lines(ReportBuilder.build(new Simulation(ReportFixtures.DT), ReportFixtures.NOW));

        assertEquals(I18n.get("report.word.empty"), lines.get(lines.size() - 1));
        assertTrue(lines.stream().noneMatch(l -> l.startsWith("+")));
    }

    @Test
    void renderWritesUtf8AndUsesTheTxtExtension() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TextReportRenderer renderer = new TextReportRenderer();

        renderer.render(ReportFixtures.sampleReport(), out);

        assertTrue(out.toString(StandardCharsets.UTF_8).contains(I18n.get("report.title")));
        assertEquals("trajectorium-report-20261108-1230.txt", renderer.defaultFileName(ReportFixtures.NOW));
    }
}