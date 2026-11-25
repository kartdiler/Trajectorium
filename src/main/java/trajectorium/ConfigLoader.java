package trajectorium;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConfigLoader {
    private ConfigLoader() {}

    public static final String ENV_PREFIX = "TRAJECTORIUM_";
    private static final Pattern ARGUMENT = Pattern.compile("^--(?<key>[\\w.]+)=(?<value>.*)$");
    private static final Pattern PROFILE_NAME = Pattern.compile("^[a-z0-9][a-z0-9_-]{0,19}$");
    private static final Set<String> COMMAND_FLAGS = Set.of("--print-config", "--init-config", "--headless");
    private static final Set<String> COMMAND_OPTIONS = Set.of("scene", "years", "steps", "format", "out");

    @FunctionalInterface
    public interface ResourceOpener {
        InputStream open(String path) throws IOException;
    }

    public record Loaded(Properties properties, Map<String, String> sources, List<String> warnings) {}

    public static Loaded loadDefault(List<String> args) {
        Path userFile = UserDirs.resolve(System.getenv(), System.getProperties()).configFile();
        return load(args, System.getenv(), userFile, ConfigLoader.class::getResourceAsStream);
    }

    public static Loaded load(List<String> args, Map<String, String> env, Path userFile, ResourceOpener resources) {
        Properties merged = new Properties();
        Map<String, String> sources = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();

        Properties bundled = readResource(resources, "/config.properties", warnings);
        if (bundled == null) {
            warnings.add("Встроенный config.properties не найден, используются значения по умолчанию");
        }
        overlay(merged, sources, bundled, "config.properties (встроенный)", warnings);

        String profile = profileName(args, env, warnings);
        if (profile != null) {
            Properties layer = readResource(resources, "/config-" + profile + ".properties", warnings);
            if (layer == null) {
                warnings.add("Профиль \"" + profile + "\" не найден: нет файла config-" + profile + ".properties");
            }
            overlay(merged, sources, layer, "профиль " + profile, warnings);
        }

        overlay(merged, sources, readFile(userFile, warnings), "пользовательский файл", warnings);

        for (String key: AppConfig.KNOWN_KEYS) {
            String name = envName(key);
            String value = env.get(name);
            if (value != null) {
                merged.setProperty(key, value.trim());
                sources.put(key, "переменная окружения " + name);
            }
        }

        for (String arg : args) {
            if (COMMAND_FLAGS.contains(arg)) continue;
            Matcher m = ARGUMENT.matcher(arg);
            if (!m.matches()) {
                warnings.add("Неизвестный аргумент: " + arg);
                continue;
            }
            String key = m.group("key");
            if (key.equals("profile") || COMMAND_OPTIONS.contains(key)) continue;
            if (!AppConfig.KNOWN_KEYS.contains(key)) {
                warnings.add("Неизвестный параметр командной строки: --" + key);
                continue;
            }
            merged.setProperty(key, m.group("value").trim());
            sources.put(key, "аргумент --" + key);
        }

        return new Loaded(merged, Collections.unmodifiableMap(sources), Collections.unmodifiableList(warnings));
    }

    static String envName(String key) {
        return ENV_PREFIX + key.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "_");
    }

    private static Properties readResource(ResourceOpener resources, String path, List<String> warnings) {
        try (InputStream in = resources.open(path)) {
            if (in == null) {
                return null;
            }
            Properties p = new Properties();
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return p;
        } catch (IOException e) {
            warnings.add("Не удалось прочитать " + path + ": " + e.getMessage());
            return null;
        }
    }

    private static Properties readFile(Path file, List<String> warnings) {
        if (file == null || !Files.isRegularFile(file)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Properties p = new Properties();
            p.load(reader);
            return p;
        } catch (IOException e) {
            warnings.add("Не удалось прочитать " + file + ": " + e.getMessage());
            return null;
        }
    }

    private static void overlay(Properties target, Map<String, String> sources, Properties layer,
                                String label, List<String> warnings) {
        if (layer == null) return;
        for (String key : layer.stringPropertyNames()) {
            if (!AppConfig.KNOWN_KEYS.contains(key)) {
                warnings.add("Неизвестный параметр \"" + key + "\" в источнике \"" + label + "\"");
                continue;
            }
            target.setProperty(key, layer.getProperty(key).trim());
            sources.put(key, label);
        }
    }

    private static String profileName(List<String> args, Map<String, String> env, List<String> warnings) {
        String raw = null;
        for (String arg : args) {
            if (arg.startsWith("--profile=")) {
                raw = arg.substring("--profile=".length());
            }
        }
        if (raw == null) {
            raw = env.get(ENV_PREFIX + "PROFILE");
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }

        raw = raw.trim();
        if (!PROFILE_NAME.matcher(raw).matches()) {
            warnings.add("Недопустимое имя профиля \"" + raw + "\": допускаются строчные латинские буквы, цифры, _ и -");
            return null;
        }
        return raw;
    }
}
