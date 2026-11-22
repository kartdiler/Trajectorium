package trajectorium.report;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static trajectorium.report.TextTable.Align.LEFT;
import static trajectorium.report.TextTable.Align.RIGHT;

public class TextTableTest {

    @Test
    void rendersBorderedTableWithAlignedColumns() {
        List<String> lines = TextTable.render(
                List.of("Имя", "Масса"),
                List.of(List.of("Sun", "1.0"), List.of("Earth", "3.0e-6")),
                List.of(LEFT, RIGHT), 20);

        assertEquals(List.of(
                "+-------+--------+",
                "| Имя   | Масса  |",
                "+-------+--------+",
                "| Sun   |    1.0 |",
                "| Earth | 3.0e-6 |",
                "+-------+--------+"), lines);
    }

    @Test
    void everyLineHasTheSameLength() {
        List<String> lines = TextTable.render(List.of("A", "BBBB"),
                List.of(List.of("длинное значение", "1"), List.of("x", "22222")), List.of(LEFT, RIGHT), 40);

        assertEquals(1, lines.stream().mapToInt(String::length).distinct().count());
    }

    @Test
    void longCellsAreTruncatedWithATilde() {
        List<String> lines = TextTable.render(List.of("Имя"), List.of(List.of("очень-длинное-имя-тела")),
                List.of(LEFT), 10);

        assertEquals("| очень-дли~ |", lines.get(3));
    }

    @Test
    void emptyTableHasOnlyHeader() {
        List<String> lines = TextTable.render(List.of("A"), List.of(), List.of(LEFT), 10);
        assertEquals(List.of("+---+", "| A |", "+---+", "+---+"), lines);
    }

    @Test
    void mismatchedRowsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> TextTable.render(
                List.of("A", "B"), List.of(List.of("1")), List.of(LEFT, LEFT), 10));
        assertThrows(IllegalArgumentException.class, () -> TextTable.render(
                List.of("A", "B"), List.of(), List.of(LEFT), 10));
    }
}