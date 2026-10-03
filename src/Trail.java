import java.util.ArrayDeque;
import java.util.Deque;

public class Trail {
    private final Deque<double[]> points = new ArrayDeque<double[]>();
    private int maxPoints;
    private boolean enabled = true;

    public Trail(int maxPoints) {
        this.maxPoints = maxPoints;
    }

    public void addPoint(double x, double y) {
        if (!enabled) return;

        points.addLast(new double[]{x, y});
        if (points.size() > maxPoints) {
            points.removeFirst();
        }
    }

    public Deque<double[]> getPoints() {
        return points;
    }

    public void clear() {
        points.clear();
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(int maxPoints) {
        this.maxPoints = maxPoints;
        while (points.size() > maxPoints) {
            points.removeFirst();
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) clear();
    }

    public boolean isEnabled() {
        return enabled;
    }
}
