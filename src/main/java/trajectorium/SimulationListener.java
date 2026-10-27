package trajectorium;

public interface SimulationListener {
    default void onStep(Simulation sim) {}
    default void onBodyAdded(Celestial body) {}
    default void onBodyRemoved(Celestial body) {}
    default void onCollision(Celestial removed, Celestial stayed) {}
    default void onRunningChanged(boolean running) {}
    default void onReset() {}
    default void onSpeedChanged(double speed) {}
    default void onIntegratorChanged(Integrator integrator) {}
}
