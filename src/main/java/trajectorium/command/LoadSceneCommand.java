package trajectorium.command;

import trajectorium.CelestialFactory;
import trajectorium.Simulation;
import trajectorium.io.SceneData;
import trajectorium.io.SceneMapper;

public class LoadSceneCommand implements Command {
    private final Simulation sim;
    private final CelestialFactory factory;
    private final SceneData data;
    private SceneData previous;

    public LoadSceneCommand(Simulation sim, CelestialFactory factory, SceneData data) {
        this.sim = sim;
        this.factory = factory;
        this.data = data;
    }

    @Override
    public void execute() {
        previous = SceneMapper.capture(sim);
        SceneMapper.apply(data, sim, factory);
    }

    @Override
    public void undo() {
        SceneMapper.apply(previous, sim, factory);
    }

    @Override
    public String getName() {
        return "Загрузить сцену";
    }
}
