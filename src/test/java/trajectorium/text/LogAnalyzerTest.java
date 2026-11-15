package trajectorium.text;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import trajectorium.CelestialFactory;
import trajectorium.Simulation;
import trajectorium.i18n.I18n;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class LogAnalyzerTest {
    private static final String SAMPLE = """
            2026-11-10 10:15:00.123 [INFO] t.Simulation - Тело добавлено: Sun (масса 1.0 M☉)
            2026-11-10 10:15:00.125 [INFO] t.Simulation - Тело добавлено: Rock (масса 3.003E-6 M☉)
            2026-11-10 10:15:05.000 [INFO] t.CommandHistory - Команда: Пауза
            2026-11-10 10:15:05.001 [INFO] t.Simulation - Симуляция приостановлена
            2026-11-10 10:15:09.500 [INFO] t.Simulation - Столкновение: Rock удалено при ударе о Sun
            2026-11-10 10:16:00.000 [WARN] t.AppConfig - Неверное значение view.scale='abc', используется 100.0
            2026-11-10 10:16:30.000 [ERROR] t.SceneFileController - Не удалось открыть сцену
            java.io.IOException: boom
                at trajectorium.X.y(X.java:1)
            2026-11-10 10:17:00.250 [INFO] t.ReportFileController - Отчёт сохранён: C:\\r.xlsx (3 тел, 7 КБ, 42 мс)
            2026-11-10 10:17:10.000 [INFO] t.ReportFileController - Отчёт сохранён: C:\\r.docx (3 тел, 40 КБ, 58 мс)
            """;

    @AfterEach
    void restoreRussian() {
        I18n.setLocale(I18n.RU);
    }

    private static LogSummary analyze(String text) throws IOException {
        return LogAnalyzer.analyze(new BufferedReader(new StringReader(text)));
    }

    @Test
    void countsLinesEntriesAndStackTraceContinuations() throws IOException {
        LogSummary s = analyze(SAMPLE);

        assertEquals(11, s.totalLines());
        assertEquals(9, s.entries());
        assertEquals(2, s.continuationLines());
        assertEquals(0, s.unparsedLines());
        assertEquals(7, s.byLevel().get("INFO"));
        assertEquals(1, s.byLevel().get("WARN"));
        assertEquals(1, s.byLevel().get("ERROR"));
        assertEquals("ERROR", s.byLevel().keySet().iterator().next());
    }

    @Test
    void periodDatesAreReformattedWithGroups() throws IOException {
        LogSummary s = analyze(SAMPLE);

        assertEquals("10.11.2026 10:15:00.123", s.firstTime());
        assertEquals("10.11.2026 10:17:10.000", s.lastTime());
        assertEquals("10.11.2026", LogAnalyzer.reformatDate("2026-11-10"));
    }

    @Test
    void extractsCollisionsCommandsAndAddedBodies() throws IOException {
        LogSummary s = analyze(SAMPLE);

        assertEquals(1, s.collisions().size());
        assertEquals("Rock", s.collisions().get(0).removed());
        assertEquals("Sun", s.collisions().get(0).survivor());
        assertEquals("10.11.2026 10:15:09.500", s.collisions().get(0).time());

        assertEquals(1, s.commands().get("Пауза"));

        assertEquals(2, s.addedBodies().size());
        assertEquals("Rock", s.addedBodies().get(1).name());
        assertEquals(3.003e-6, s.addedBodies().get(1).mass(), 1e-18);
    }

    @Test
    void averagesExportTimeAndCollectsProblems() throws IOException {
        LogSummary s = analyze(SAMPLE);

        assertEquals(2, s.exportCount());
        assertEquals(50.0, s.averageExportMillis(), 1e-9);
        assertEquals(2, s.problems().size());
        assertEquals("WARN", s.problems().get(0).level());
        assertEquals("ERROR", s.problems().get(1).level());
    }

    @Test
    void emptyAndGarbageInputIsHandled() throws IOException {
        LogSummary empty = analyze("");
        assertEquals(0, empty.entries());
        assertNull(empty.firstTime());
        assertTrue(empty.byLevel().isEmpty());

        LogSummary garbage = analyze("просто текст\nещё строка\n\n2026-11-10 10:00:00.000 [INFO] t.X - привет\n");
        assertEquals(2, garbage.unparsedLines());
        assertEquals(1, garbage.entries());
    }

    @Test
    void analyzesFileWithBrokenBytes(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("broken.log");
        byte[] good = "2026-11-10 10:00:00.000 [INFO] t.X - привет\n".getBytes(StandardCharsets.UTF_8);
        byte[] bad = {(byte) 0xC3, (byte) 0x28, '\n'};
        byte[] all = new byte[good.length + bad.length];
        System.arraycopy(good, 0, all, 0, good.length);
        System.arraycopy(bad, 0, all, good.length, bad.length);
        Files.write(file, all);

        LogSummary s = LogAnalyzer.analyze(file);

        assertEquals(1, s.entries());
    }

    @Test
    void collisionPatternMatchesTheMessageWrittenBySimulation() {
        Logger logger = (Logger) LoggerFactory.getLogger(Simulation.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            Simulation sim = new Simulation(1.0 / 365.25);
            CelestialFactory factory = new CelestialFactory(sim, 100);
            factory.createStar("Sun", 1.0, 0, 0, 20, Color.YELLOW);
            factory.createFree("Rock", 1e-6, CelestialFactory.MassUnit.SOLAR_MASSES, 0.1, 0, 0, 0, 5, Color.GRAY);
            sim.step();

            String message = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .filter(m -> m.startsWith("Столкновение"))
                    .findFirst().orElseThrow();

            LogSummary.Collision c = LogAnalyzer.parseCollision("t", message).orElseThrow();
            assertEquals("Rock", c.removed());
            assertEquals("Sun", c.survivor());
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void summaryTextFollowsTheLanguage() throws IOException {
        LogSummary s = analyze(SAMPLE);

        String ru = LogSummaryFormatter.format(s, "trajectorium.log");
        assertTrue(ru.contains("Столкновения (1):"), ru);
        assertTrue(ru.contains("10.11.2026 10:15:09.500: Rock удалено при ударе о Sun"), ru);

        I18n.setLocale(I18n.EN);
        String en = LogSummaryFormatter.format(s, "trajectorium.log");
        assertTrue(en.contains("Collisions (1):"), en);
        assertTrue(en.contains("Rock removed after hitting Sun"), en);
    }
}