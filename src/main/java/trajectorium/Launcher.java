package trajectorium;

import javafx.application.Application;
import org.slf4j.LoggerFactory;
import trajectorium.i18n.I18n;
import trajectorium.text.LogCli;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class Launcher {
    static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) ->
                LoggerFactory.getLogger("trajectorium.Uncaught")
                        .error("Необработанная ошибка в потоке {}", thread.getName(), error));

        List<String> list = List.of(args);

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
}
