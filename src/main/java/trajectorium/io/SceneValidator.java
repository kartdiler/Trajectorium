package trajectorium.io;

import trajectorium.i18n.I18n;

import java.util.List;
import java.util.regex.Pattern;

public final class SceneValidator {
    public static final int MAX_BODIES = 500;
    public static final double MAX_ABS = 1e6;
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private SceneValidator() {}

    public static void validate(SceneData d) throws SceneFormatException {
        if (d == null) {
            throw new SceneFormatException(I18n.get("error.empty"));
        }

        if (d.version() != SceneData.CURRENT_VERSION) {
            throw new SceneFormatException(I18n.format("error.version", d.version()));
        }

        if (!SceneMapper.isKnownIntegrator(d.integrator())) {
            throw new SceneFormatException(I18n.format("error.integrator", d.integrator()));
        }

        if (!inRange(d.simulationSpeed(), 0.1, 100)) {
            throw new SceneFormatException(I18n.get("error.speed"));
        }

        List<BodyData> bodies = d.bodies();
        if (bodies == null) {
            throw new SceneFormatException(I18n.get("error.bodies.missing"));
        }

        if (bodies.size() > MAX_BODIES) {
            throw new SceneFormatException(I18n.format("error.bodies.many", bodies.size(), MAX_BODIES));
        }

        for (int i = 0; i < bodies.size(); i++) {
            validateBody(i + 1, bodies.get(i));
        }

        for (int i = 0; i < bodies.size(); i++) {
            for (int j = i + 1; j < bodies.size(); j++) {
                BodyData a = bodies.get(i);
                BodyData b = bodies.get(j);
                if (Math.hypot(a.x() - b.x(), a.y() - b.y()) < 1e-9) {
                    throw new SceneFormatException(I18n.format("error.samepoint", a.name(), b.name()));
                }
            }
        }
    }

    private static void validateBody(int number, BodyData b) throws SceneFormatException {
        if (b == null) {
            throw new SceneFormatException(I18n.format("error.body.empty", number));
        }

        String where = "Тело №" + number + ": ";
        if (b.name() == null || b.name().isBlank() || b.name().length() > 50) {
            throw new SceneFormatException(I18n.format("error.body.name", number));
        }

        if (!(Double.isFinite(b.mass()) && b.mass() > 0 && b.mass() <= MAX_ABS)) {
            throw new SceneFormatException(I18n.format("error.body.mass", number));
        }

        for (double v : new double[] {b.x(), b.y(), b.vx(), b.vy()}) {
            if (!inRange(v, -MAX_ABS, MAX_ABS)) {
                throw new SceneFormatException(I18n.format("error.body.numbers", number));
            }
        }

        if (!(Double.isFinite(b.canvasRadius()) && b.canvasRadius() > 0 && b.canvasRadius() <= 100)) {
            throw new SceneFormatException(I18n.format("error.body.radius", number));
        }

        if (b.color() == null || !HEX_COLOR.matcher(b.color()).matches()) {
            throw new SceneFormatException(I18n.format("error.body.color", number));
        }
    }

    private static boolean inRange(double v, double min, double max) {
        return Double.isFinite(v) && v >= min && v <= max;
    }
}
