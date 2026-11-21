package trajectorium;

import java.util.List;
import java.util.Optional;

public final class Placement {
    private Placement() {}

    public static Optional<Celestial> findOverlap(List<Celestial> bodies, double x, double y, double realRadius) {
        for (Celestial c : bodies) {
            double[] p = c.getPos();
            if (Math.hypot(x - p[0], y - p[1]) < c.getRealRadius() + realRadius) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }
}
