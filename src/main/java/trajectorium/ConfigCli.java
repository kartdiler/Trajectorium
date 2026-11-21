package trajectorium;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class ConfigCli {
    private ConfigCli() {}

    public static int print(List<String> args, PrintStream out) {
        return print(ConfigLoader.loadDefault(args), out);
    }

    public static int print(ConfigLoader.Loaded loaded, PrintStream out) {
        AppConfig config = new AppConfig(loaded.properties());
        for (Map.Entry<String, Object> e : config.asMap().entrySet()) {
            String source = loaded.sources().getOrDefault(e.getKey(), "по умолчанию");
            out.printf("%-22s = %-10s [%s]%n", e.getKey(), e.getValue(), source);
        }
        loaded.warnings().forEach(w -> out.println("! " + w));
        return 0;
    }

    public static int init(Path file, PrintStream out) {
        if (Files.exists(file)) {
            out.println("Файл уже существует: " + file);
            return 1;
        }

        StringBuilder text = new StringBuilder()
                .append("# Пользовательская конфигурация Trajectorium.\n")
                .append("# Приоритет: аргументы > переменные окружения TRAJECTORIUM_* > этот файл > профиль > встроенный файл.\n")
                .append("# Уберите \"#\" у строки, чтобы переопределить значение.\n\n");

        new AppConfig(new Properties()).asMap().forEach((key, value) -> text.append('#').append(key).append('=').append(value).append('\n'));
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            out.println("Не удалось создать файл " + file + ": " + e.getMessage());
            return 2;
        }
        out.println("Создан файл: " + file);
        return 0;
    }
}
