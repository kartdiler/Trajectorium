package trajectorium.io;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ManyBodiesSceneTest {

    @Test
    void writesSceneWithEightyBodiesAndReadsItBack() throws Exception {
        List<BodyData> bodies = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            bodies.add(new BodyData("Body-" + i, 1e-9, 1 + 0.5 * i, (i % 2 == 0) ? 0 : 2, 0, 0, 2, "#808080"));
        }
        SceneData data = new SceneData(SceneData.CURRENT_VERSION, "Verlet", 1.0, bodies);
        Path file = Path.of("target", "many-bodies.json");
        Files.createDirectories(file.getParent());

        SceneStorage.save(file, data);

        assertEquals(80, SceneStorage.load(file).bodies().size());
    }
}