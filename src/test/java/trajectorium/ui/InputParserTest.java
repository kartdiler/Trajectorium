package trajectorium.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class InputParserTest {
    @Test
    void parserPlainDecimalCommaAndScientific() {
        assertEquals(3.5, InputParser.parseNumber("3.5").getAsDouble(), 1e-12);
        assertEquals(3.5, InputParser.parseNumber("3,5").getAsDouble(), 1e-12);
        assertEquals(1e-5, InputParser.parseNumber("1e-5").getAsDouble(), 1e-18);
        assertEquals(-2500, InputParser.parseNumber("-2.5E3").getAsDouble(), 1e-12);
        assertEquals(0.5, InputParser.parseNumber(".5").getAsDouble(), 1e-12);
        assertEquals(5.0, InputParser.parseNumber(" 5. ").getAsDouble(), 1e-12);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "abc", "1.2.3", "1e", "--1", "Nan", "Infinity", "1e999", "5 5"})
    void rejectsInvalidNumbers(String input) {
        assertTrue(InputParser.parseNumber(input).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Земля", "Planet-1", "Тело 2", "A", "Mars_2"})
    void acceptsValidNames(String name) {
        assertTrue(InputParser.isValidName(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "-x", "bad!na", "aaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void rejectsInvalidNames(String name) {
        assertFalse(InputParser.isValidName(name));
    }
}
