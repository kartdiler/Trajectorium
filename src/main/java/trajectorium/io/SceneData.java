package trajectorium.io;

import java.util.List;

public record SceneData(int version, String integrator, double simulationSpeed, List<BodyData> bodies) {
    public static final int CURRENT_VERSION = 1;
}
