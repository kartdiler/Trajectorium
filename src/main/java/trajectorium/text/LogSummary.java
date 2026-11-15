package trajectorium.text;

import java.util.List;
import java.util.Map;

public record LogSummary(int totalLines, int entries, int continuationLines, int unparsedLines,
                         Map<String, Integer> byLevel, List<Collision> collisions,
                         Map<String, Integer> commands, List<AddedBody> addedBodies,
                         int exportCount, double averageExportMillis,
                         String firstTime, String lastTime, List<Problem> problems) {

    public record Collision(String time, String removed, String survivor) {}

    public record AddedBody(String name, double mass) {}

    public record Problem(String time, String level, String message) {}
}