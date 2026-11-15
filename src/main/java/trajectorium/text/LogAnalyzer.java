package trajectorium.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LogAnalyzer {
    private LogAnalyzer() {}

    private static final Pattern ENTRY = Pattern.compile(
            "^(?<date>\\d{4}-\\d{2}-\\d{2}) (?<time>\\d{2}:\\d{2}:\\d{2}\\.\\d{3}) "
                    + "\\[(?<level>[A-Z]+)] (?<logger>\\S+) - (?<message>.*)$");

    private static final Pattern DATE = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
    private static final Pattern COLLISION =
            Pattern.compile("^Столкновение: (?<removed>.+) удалено при ударе о (?<survivor>.+)$");
    private static final Pattern COMMAND = Pattern.compile("^Команда: (?<name>.+)$");
    private static final Pattern BODY_ADDED = Pattern.compile("^Тело добавлено: (?<name>.+?) \\(масса ");
    private static final Pattern MASS_AFTER_LABEL = Pattern.compile("(?<=масса )[\\d.eE+-]+");
    private static final Pattern EXPORT = Pattern.compile(
            "^Отчёт сохранён: .+ \\((?<bodies>\\d+) тел, (?<kb>\\d+) КБ, (?<ms>\\d+) мс\\)$");

    private static final List<String> LEVEL_ORDER = List.of("ERROR", "WARN", "INFO", "DEBUG", "TRACE");
    private static final int MAX_PROBLEMS = 20;

    public static String reformatDate(String isoDate) {
        return DATE.matcher(isoDate).replaceAll("$3.$2.$1");
    }

    static Optional<LogSummary.Collision> parseCollision(String time, String message) {
        Matcher m = COLLISION.matcher(message);
        return m.matches()
                ? Optional.of(new LogSummary.Collision(time, m.group("removed"), m.group("survivor")))
                : Optional.empty();
    }

    public static LogSummary analyze(Path file) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8))) {
            return analyze(reader);
        }
    }

    public static LogSummary analyze(BufferedReader reader) throws IOException {
        int total = 0;
        int entries = 0;
        int continuation = 0;
        int unparsed = 0;
        boolean inEntry = false;

        Map<String, Integer> levels = new HashMap<>();
        List<LogSummary.Collision> collisions = new ArrayList<>();
        Map<String, Integer> commands = new LinkedHashMap<>();
        List<LogSummary.AddedBody> added = new ArrayList<>();
        List<LogSummary.Problem> problems = new ArrayList<>();
        int exportCount = 0;
        long exportMillis = 0;
        String first = null;
        String last = null;

        String line;
        while ((line = reader.readLine()) != null) {
            total++;
            Matcher entry = ENTRY.matcher(line);
            if (!entry.matches()) {
                if (line.isBlank()) continue;
                if (inEntry) continuation++; else unparsed++;
                continue;
            }

            inEntry = true;
            entries++;
            String time = reformatDate(entry.group("date")) + " " + entry.group("time");
            String level = entry.group("level");
            String message = entry.group("message");
            if (first == null) first = time;
            last = time;
            levels.merge(level, 1, Integer::sum);

            if (level.equals("WARN") || level.equals("ERROR")) {
                if (problems.size() < MAX_PROBLEMS) problems.add(new LogSummary.Problem(time, level, message));
            }

            Optional<LogSummary.Collision> collision = parseCollision(time, message);
            if (collision.isPresent()) {
                collisions.add(collision.get());
                continue;
            }
            Matcher command = COMMAND.matcher(message);
            if (command.matches()) {
                commands.merge(command.group("name"), 1, Integer::sum);
                continue;
            }
            Matcher body = BODY_ADDED.matcher(message);
            if (body.find()) {
                Matcher mass = MASS_AFTER_LABEL.matcher(message);
                if (mass.find()) {
                    try {
                        added.add(new LogSummary.AddedBody(body.group("name"), Double.parseDouble(mass.group())));
                    } catch (NumberFormatException ignored) {}
                }
                continue;
            }
            Matcher export = EXPORT.matcher(message);
            if (export.matches()) {
                exportCount++;
                exportMillis += Long.parseLong(export.group("ms"));
            }
        }

        return new LogSummary(total, entries, continuation, unparsed, orderedLevels(levels),
                List.copyOf(collisions), Collections.unmodifiableMap(commands), List.copyOf(added),
                exportCount, exportCount == 0 ? 0 : (double) exportMillis / exportCount,
                first, last, List.copyOf(problems));
    }

    private static Map<String, Integer> orderedLevels(Map<String, Integer> counts) {
        Map<String, Integer> ordered = new LinkedHashMap<>();
        for (String level : LEVEL_ORDER) {
            if (counts.containsKey(level)) ordered.put(level, counts.get(level));
        }
        new TreeMap<>(counts).forEach(ordered::putIfAbsent);
        return Collections.unmodifiableMap(ordered);
    }
}