package trajectorium;

import java.util.List;

public interface Integrator {
    void step(List<Celestial> bodies, double dt);
    String getName();
}
