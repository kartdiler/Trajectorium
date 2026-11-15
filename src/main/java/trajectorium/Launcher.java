package trajectorium;

import javafx.application.Application;
import trajectorium.i18n.I18n;
import trajectorium.text.LogCli;

import java.nio.file.Path;
import java.util.Locale;

public class Launcher {
    static void main(String[] args) {
        if (args.length == 2 && args[0].equals("--analyze-log")) {
            I18n.setLocale(Locale.forLanguageTag(AppConfig.load().getLanguage()));
            System.exit(LogCli.run(Path.of(args[1]), System.out, System.err));
        }
        Application.launch(Main.class, args);
    }
}
