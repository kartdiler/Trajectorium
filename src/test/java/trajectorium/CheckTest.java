package trajectorium;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class CheckTest {

    @Test
    void notNullReturnsTheValueOrNamesTheParameter() {
        assertEquals("x", Check.notNull("x", "имя"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Check.notNull(null, "список"));
        assertTrue(e.getMessage().contains("\"список\""), e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void positiveRejectsZeroNegativeAndNonFiniteValues(double value) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Check.positive(value, "масса"));
        assertTrue(e.getMessage().contains("\"масса\""));
        assertTrue(e.getMessage().contains(String.valueOf(value)), e.getMessage());
    }

    @Test
    void positiveAcceptsTinyAndLargeValues() {
        assertEquals(1e-300, Check.positive(1e-300, "x"), 0.0);
        assertEquals(1e300, Check.positive(1e300, "x"), 0.0);
    }

    @Test
    void positiveAtMostChecksBothBounds() {
        assertEquals(5.0, Check.positiveAtMost(5, 10, "x"), 0.0);
        assertEquals(10.0, Check.positiveAtMost(10, 10, "x"), 0.0);
        assertThrows(IllegalArgumentException.class, () -> Check.positiveAtMost(10.0001, 10, "x"));
        assertThrows(IllegalArgumentException.class, () -> Check.positiveAtMost(0, 10, "x"));
    }

    @Test
    void inRangeIncludesBoundsAndRejectsNaN() {
        assertEquals(-5.0, Check.inRange(-5, -5, 5, "x"), 0.0);
        assertEquals(5.0, Check.inRange(5, -5, 5, "x"), 0.0);
        assertThrows(IllegalArgumentException.class, () -> Check.inRange(5.1, -5, 5, "x"));
        assertThrows(IllegalArgumentException.class, () -> Check.inRange(Double.NaN, -5, 5, "x"));
    }

    @Test
    void textRejectsBlankAndTooLong() {
        assertEquals("Земля", Check.text("Земля", "имя", 10));
        assertThrows(IllegalArgumentException.class, () -> Check.text(null, "имя", 10));
        assertThrows(IllegalArgumentException.class, () -> Check.text("   ", "имя", 10));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Check.text("12345678901", "имя", 10));
        assertTrue(e.getMessage().contains("10"), e.getMessage());
    }

    @Test
    void atLeastRejectsSmallerIntegers() {
        assertEquals(0, Check.atLeast(0, 0, "n"));
        assertThrows(IllegalArgumentException.class, () -> Check.atLeast(-1, 0, "n"));
    }
}