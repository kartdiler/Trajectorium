package trajectorium;

import javafx.application.Application;
import org.slf4j.LoggerFactory;
import trajectorium.headless.HeadlessOptions;
import trajectorium.headless.HeadlessRunner;
import trajectorium.i18n.I18n;
import trajectorium.text.LogCli;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Launcher {
    private static final Set<String> CLI_FLAGS = Set.of("--headless", "--print-config", "--init-config", "--analyze-log");

    static void main(String[] args) {
        List<String> list = List.of(args);
        if (list.stream().anyMatch(CLI_FLAGS::contains)) {
            System.setProperty("LOG_TARGET", "System.err");
        }
        Thread.setDefaultUncaughtExceptionHandler((thread, error) ->
                LoggerFactory.getLogger("trajectorium.Uncaught")
                        .error("Необработанная ошибка в потоке {}", thread.getName(), error));

        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8);

        if (list.contains("--headless")) {
            System.exit(headless(list, out, err));
        }
        if (list.contains("--print-config")) {
            System.exit(ConfigCli.print(list, System.out));
        }
        if (list.contains("--init-config")) {
            System.exit(ConfigCli.init(UserDirs.resolve(System.getenv(), System.getProperties()).configFile(), System.out));
        }
        if (list.size() == 2 && list.get(0).equals("--analyze-log")) {
            I18n.setLocale(Locale.forLanguageTag(AppConfig.load().getLanguage()));
            System.exit(LogCli.run(Path.of(args[1]), System.out, System.err));
        }
        Application.launch(Main.class, args);
    }

    private static int headless(List<String> args, PrintStream out, PrintStream err) {
        AppConfig config = AppConfig.load(args);
        I18n.setLocale(Locale.forLanguageTag(config.getLanguage()));
        try {
            return HeadlessRunner.run(HeadlessOptions.parse(args), config, out, err);
        } catch (HeadlessOptions.UsageException e) {
            err.println(e.getMessage());
            err.println(HeadlessOptions.USAGE);
            return HeadlessRunner.USAGE;
        }
    }
}
