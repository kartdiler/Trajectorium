package trajectorium.report;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PagePlannerTest {

    private static List<String> lines(int count, int width) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < count; i++) list.add(String.valueOf(i % 10).repeat(width));
        return list;
    }

    @Test
    void fontIsFittedToTheWidestLineAndPagesAreCut() {
        PagePlanner.Plan plan = PagePlanner.plan(lines(150, 100), 500, 700, 0.6, 1.2);

        assertEquals(8.0, plan.fontSize(), 0.0);
        assertEquals(9.6, plan.lineHeight(), 1e-9);
        assertEquals(70, plan.linesPerPage());                          // 72 строки минус две на подпись
        assertEquals(List.of(70, 70, 10), plan.pages().stream().map(List::size).toList());
    }

    @Test
    void fontIsClampedToTheLimits() {
        assertEquals(PagePlanner.MAX_FONT, PagePlanner.plan(lines(3, 5), 500, 700, 0.6, 1.2).fontSize(), 0.0);
        assertEquals(PagePlanner.MIN_FONT, PagePlanner.plan(lines(3, 400), 500, 700, 0.6, 1.2).fontSize(), 0.0);
    }

    @Test
    void noLineIsLostOrReordered() {
        List<String> input = new ArrayList<>();
        for (int i = 0; i < 333; i++) input.add("строка " + i);

        PagePlanner.Plan plan = PagePlanner.plan(input, 400, 500, 0.6, 1.2);

        List<String> joined = plan.pages().stream().flatMap(List::stream).toList();
        assertEquals(input, joined);
        assertTrue(plan.pages().stream().allMatch(p -> p.size() <= plan.linesPerPage()));
    }

    @Test
    void emptyReportStillHasOnePage() {
        PagePlanner.Plan plan = PagePlanner.plan(List.of(), 500, 700, 0.6, 1.2);
        assertEquals(1, plan.pages().size());
        assertTrue(plan.pages().get(0).isEmpty());
    }

    @Test
    void tinyPageStillGetsOneLine() {
        PagePlanner.Plan plan = PagePlanner.plan(lines(5, 10), 500, 5, 0.6, 1.2);
        assertEquals(1, plan.linesPerPage());
        assertEquals(5, plan.pages().size());
    }

    @Test
    void invalidMetricsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> PagePlanner.plan(List.of(), 0, 700, 0.6, 1.2));
        assertThrows(IllegalArgumentException.class, () -> PagePlanner.plan(List.of(), 500, -1, 0.6, 1.2));
        assertThrows(IllegalArgumentException.class, () -> PagePlanner.plan(null, 500, 700, 0.6, 1.2));
    }
}