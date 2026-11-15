package trajectorium.text;

import trajectorium.i18n.I18n;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

public final class LogCli {
    private LogCli() {}

    public static int run(Path file, PrintStream out, PrintStream err) {
        try {
            LogSummary summary = LogAnalyzer.analyze(file);
            out.print(LogSummaryFormatter.format(summary, String.valueOf(file.getFileName())));
            return 0;
        } catch (IOException e) {
            err.println(I18n.get("log.error.read") + ": " + e.getMessage());
            return 1;
        }
    }
}