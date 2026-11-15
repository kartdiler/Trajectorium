package trajectorium.text;

import trajectorium.i18n.I18n;

import java.util.Comparator;
import java.util.Map;

public final class LogSummaryFormatter {
    private LogSummaryFormatter() {}

    public static String format(LogSummary s, String fileName) {
        StringBuilder out = new StringBuilder();
        out.append(I18n.format("log.summary.file", fileName)).append('\n');
        out.append(I18n.format("log.summary.lines", s.totalLines(), s.entries(),
                s.continuationLines(), s.unparsedLines())).append('\n');
        if (s.firstTime() != null) {
            out.append(I18n.format("log.summary.period", s.firstTime(), s.lastTime())).append('\n');
        }

        if (!s.byLevel().isEmpty()) {
            out.append('\n').append(I18n.get("log.summary.levels")).append('\n');
            for (Map.Entry<String, Integer> e : s.byLevel().entrySet()) {
                out.append("  ").append(I18n.format("log.summary.level", e.getKey(), e.getValue())).append('\n');
            }
        }
        if (!s.collisions().isEmpty()) {
            out.append('\n').append(I18n.format("log.summary.collisions", s.collisions().size())).append('\n');
            for (LogSummary.Collision c : s.collisions()) {
                out.append("  ").append(I18n.format("log.summary.collision", c.time(), c.removed(), c.survivor()))
                        .append('\n');
            }
        }
        if (!s.commands().isEmpty()) {
            out.append('\n').append(I18n.get("log.summary.commands")).append('\n');
            for (Map.Entry<String, Integer> e : s.commands().entrySet()) {
                out.append("  ").append(I18n.format("log.summary.command", e.getKey(), e.getValue())).append('\n');
            }
        }
        if (!s.addedBodies().isEmpty()) {
            LogSummary.AddedBody heaviest = s.addedBodies().stream()
                    .max(Comparator.comparingDouble(LogSummary.AddedBody::mass)).orElseThrow();
            out.append('\n').append(I18n.format("log.summary.bodies", s.addedBodies().size(),
                    heaviest.name(), heaviest.mass())).append('\n');
        }
        if (s.exportCount() > 0) {
            out.append(I18n.format("log.summary.exports", s.exportCount(), s.averageExportMillis())).append('\n');
        }
        if (!s.problems().isEmpty()) {
            out.append('\n').append(I18n.format("log.summary.problems", s.problems().size())).append('\n');
            for (LogSummary.Problem p : s.problems()) {
                out.append("  ").append(I18n.format("log.summary.problem", p.time(), p.level(), p.message()))
                        .append('\n');
            }
        }
        return out.toString();
    }
}