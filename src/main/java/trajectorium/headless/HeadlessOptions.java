package trajectorium.headless;

import trajectorium.AppConfig;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record HeadlessOptions(String scene, Long steps, Double years, Format format, String out) {
    public enum Format {
        TXT, CSV, XLSX, DOCX, SCENE;

        boolean isText() {
            return this == TXT || this == CSV || this == SCENE;
        }

        static Optional<Format> of(String name) {
            for (Format f : values()) {
                if (f.name().equalsIgnoreCase(name)) {
                    return Optional.of(f);
                }
            }
            return Optional.empty();
        }
    }

    public static final long MAX_STEPS = 20_000_000L;
    public static final double MAX_YEARS = 100_000;

    public static final String USAGE = """
            Использование:
              --headless --scene=<файл.json|preset:имя> [--years=N | --steps=N]
                         [--format=txt|csv|xlsx|docx|scene] [--out=<файл>|-]
              Можно добавлять параметры конфигурации: --view.scale=.., --app.language=en, --profile=demo
            """;

    private static final Pattern ARGUMENT = Pattern.compile("^--(?<key>[\\w.-]+)=(?<value>.*)$");
    private static final Pattern PRESET = Pattern.compile("^preset:[a-z0-9-]{1,40}$");
    private static final Set<String> OWN_KEYS = Set.of("scene", "years", "steps", "format", "out");

    public static class UsageException extends Exception {
        public UsageException(String message) {
            super(message);
        }
    }

    public static HeadlessOptions parse(List<String> args) throws UsageException {
        Map<String, String> values = new HashMap<>();
        List<String> problems = new ArrayList<>();

        for (String arg : args) {
            if (arg.equals("--headless")) continue;
            Matcher m = ARGUMENT.matcher(arg);
            if (!m.matches()) {
                problems.add("Неизвестный аргумент: " + arg);
                continue;
            }
            String key = m.group("key");
            if (OWN_KEYS.contains(key)) {
                values.put(key, m.group("value")).trim();
            } else if (!key.equals("profile") && !AppConfig.KNOWN_KEYS.contains(key)) {
                problems.add("Неизвестный параметр: --" + key);
            }
        }

        String scene = values.get("scene");
        if (scene == null || scene.isEmpty()) {
            problems.add("Не задана сцена: --scene=<файл.json|preset:имя>");
        } else if (scene.startsWith("preset:") && !PRESET.matcher(scene).matches()) {
            problems.add("Недопустимое имя пресета: " + scene);
        }

        Long steps = null;
        Double years = null;
        if (values.containsKey("steps") && values.containsKey("years")) {
            problems.add("Параметры --years и --steps взаимоисключающие");
        } else if (values.containsKey("steps")) {
            steps = parseSteps(values.get("steps"), problems);
        } else if (values.containsKey("years")) {
            years = parseYears(values.get("years"), problems);
        } else {
            years = 1.0;
        }

        Format format = Format.TXT;
        if (values.containsKey("format")) {
            Optional<Format> parsed = Format.of(values.get("format"));
            if (parsed.isPresent()) {
                format = parsed.get();
            } else {
                problems.add("Неизвестный формат: " + values.get("format") + " (txt, csv, xlsx, docx, scene)");
            }
        }

        String out = values.getOrDefault("out", "-");
        if (out.isEmpty()) {
            problems.add("Пустой путь в --out");
        } else if (out.equals("-") && !format.isText()) {
            problems.add("Формат " + format.name().toLowerCase() + " нельзя вывести в консоль: укажите --out=<файл>");
        }

        if (!problems.isEmpty()) {
            throw new UsageException(String.join(System.lineSeparator(), problems));
        }

        return new HeadlessOptions(scene, steps, years, format, out);
    }

    private static Long parseSteps(String raw, List<String> problems) {
        try {
            long value = Long.parseLong(raw);
            if (value >= 1 && value <= MAX_STEPS) {
                return value;
            }
        } catch (NumberFormatException ignored) {}
        problems.add("Некорректное значение --steps: " + raw + " (нужное целое от 1 до " + MAX_STEPS + ")");
        return null;
    }

    private static Double parseYears(String raw, List<String> problems) {
        try {
            double value = Double.parseDouble(raw);
            if (Double.isFinite(value) && value > 0 && value <= MAX_YEARS) {
                return value;
            }
        } catch (NumberFormatException ignored) {}
        problems.add("Некорректное значение --years: " + raw + " (нужно число больше 0 и не больше " + (long) MAX_YEARS + ")");
        return null;
    }
}
