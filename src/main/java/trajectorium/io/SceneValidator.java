package trajectorium.io;

import java.util.List;
import java.util.regex.Pattern;

public final class SceneValidator {
    public static final int MAX_BODIES = 500;
    public static final double MAX_ABS = 1e6;
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private SceneValidator() {}

    public static void validate(SceneData d) throws SceneFormatException {
        if (d == null) {
            throw new SceneFormatException("Файл пуст");
        }

        if (d.version() != SceneData.CURRENT_VERSION) {
            throw new SceneFormatException("Неподдерживаемая версия формата: " + d.version());
        }

        if (!SceneMapper.isKnownIntegrator(d.integrator())) {
            throw new SceneFormatException("Неизвестный интегратор: " + d.integrator());
        }

        if (!inRange(d.simulationSpeed(), 0.1, 100)) {
            throw new SceneFormatException("Скорость симуляции должна быть от 0,1 до 100");
        }

        List<BodyData> bodies = d.bodies();
        if (bodies == null) {
            throw new SceneFormatException("Не задан список тел");
        }

        if (bodies.size() > MAX_BODIES) {
            throw new SceneFormatException("Слишком много тел: " + bodies.size() + " (максимум " + MAX_BODIES + ")");
        }

        for (int i = 0; i < bodies.size(); i++) {

        }
    }

    private static void validateBody(int number, BodyData b) throws SceneFormatException {
        if (b == null) {
            throw new SceneFormatException("Тело №" + number + " пустое");
        }

        String where = "Тело №" + number + ": ";
        if (b.name() == null || b.name().isBlank() || b.name().length() > 50) {
            throw new SceneFormatException(where + "имя должно содержать от 1 до 50 символов");
        }

        if (!(Double.isFinite(b.mass()) && b.mass() > 0 && b.mass() <= MAX_ABS)) {
            throw new SceneFormatException(where + "масса должна быть положительным конечным числом");
        }

        for (double v : new double[] {b.x(), b.y(), b.vx(), b.vy()}) {
            if (!inRange(v, -MAX_ABS, MAX_ABS)) {
                throw new SceneFormatException(where + "координаты и скорость должны быть конечными числами");
            }
        }

        if (!(Double.isFinite(b.canvasRadius()) && b.canvasRadius() > 0 && b.canvasRadius() <= 100)) {
            throw new SceneFormatException(where + "радиус должен быть в пределах (0; 100]");
        }

        if (b.color() == null || !HEX_COLOR.matcher(b.color()).matches()) {
            throw new SceneFormatException(where + "цвет должен быть в формате #RRGGBB");
        }
    }

    private static boolean inRange(double v, double min, double max) {
        return Double.isFinite(v) && v >= min && v <= max;
    }
}
