package com.bitaspire.sir.module.custom;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LinesTest {

    private final Random random = new Random(42);

    @Test
    void prefixesAreReadInAnyOrder() {
        Lines.Line line = Lines.parse("[repeat:3][TO:all][action_bar]&7Hi", 1000);

        assertEquals("all", line.getTo());
        assertEquals(3, line.getRepeat());
        assertEquals("[action_bar]&7Hi", line.getText());
    }

    @Test
    void linesWithoutPrefixesGoToTheSenderOnce() {
        Lines.Line line = Lines.parse("<P> &7Hello [to:all]", 1000);

        assertNull(line.getTo());
        assertEquals(1, line.getRepeat());
        assertEquals("<P> &7Hello [to:all]", line.getText());
    }

    @Test
    void repeatIsClampedAndBlankTextIsKept() {
        assertEquals(50, Lines.parse("[repeat:999]", 50).getRepeat());
        assertEquals(1, Lines.parse("[repeat:0]", 50).getRepeat());
        assertEquals(1, Lines.parse("[repeat:abc]", 50).getRepeat());
        assertEquals("", Lines.parse("[to:all][repeat:200]", 1000).getText());
    }

    @Test
    void wholeRangesIncludeBothBounds() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) seen.add(Lines.random("1-3", random));
        assertEquals(new HashSet<>(java.util.Arrays.asList("1", "2", "3")), seen);

        for (int i = 0; i < 200; i++) {
            int value = Integer.parseInt(Lines.random("-5-5", random));
            assertTrue(value >= -5 && value <= 5);
        }

        for (int i = 0; i < 200; i++) {
            int value = Integer.parseInt(Lines.random("10 - 1", random));
            assertTrue(value >= 1 && value <= 10, "reversed bounds are swapped");
        }
    }

    @Test
    void decimalRangesKeepTheDecimalsOfTheBounds() {
        for (int i = 0; i < 200; i++) {
            String value = Lines.random("0.5-2.25", random);
            assertEquals(2, value.length() - value.indexOf('.') - 1);

            double number = Double.parseDouble(value);
            assertTrue(number >= 0.5 && number <= 2.25);
        }
    }

    @Test
    void listsPickOneValue() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) seen.add(Lines.random("red| green |blue", random));
        assertEquals(new HashSet<>(java.util.Arrays.asList("red", "green", "blue")), seen);
        assertNull(Lines.random("not a spec", random));
    }

    @Test
    void evaluateRollsEachPlaceholderAndKeepsInvalidOnes() {
        String text = Lines.evaluate("{random:7-7} {random:a|a} {random:oops} {other}", random);
        assertEquals("7 a {random:oops} {other}", text);
    }

    @Test
    void calcHandlesPrecedenceParenthesesAndDecimals() {
        assertEquals("7", Lines.calc("1 + 2 * 3"));
        assertEquals("9", Lines.calc("(1+2)*3"));
        assertEquals("2.5", Lines.calc("5/2"));
        assertEquals("1", Lines.calc("10 % 3"));
        assertEquals("-4", Lines.calc("-(2+2)"));
        assertEquals("100", Lines.calc("50*2"));
        assertNull(Lines.calc("1/0"));
        assertNull(Lines.calc("2+"));
        assertNull(Lines.calc("abc"));
    }

    @Test
    void pickGivesTheValueAtTheIndex() {
        assertEquals("b", Lines.pick("2:a|b|c"));
        assertNull(Lines.pick("4:a|b|c"));
        assertNull(Lines.pick("x:a|b"));
    }

    @Test
    void evaluateResolvesNestedFunctionsInnermostFirst() {
        assertEquals("12", Lines.evaluate("{calc:{random:6-6}+{random:6-6}}", random));
        assertEquals("c", Lines.evaluate("{pick:{calc:1+2}:a|b|c}", random));
        assertEquals("{calc:{random:oops}+1}", Lines.evaluate("{calc:{random:oops}+1}", random));
    }

    @Test
    void conditionComparesNumbersAndText() {
        assertEquals("yes", Lines.condition("75>=50:yes|no"));
        assertEquals("no", Lines.condition("20 >= 50:yes|no"));
        assertEquals("", Lines.condition("1>2:yes"));
        assertEquals("same", Lines.condition("Red=red:same|other"));
        assertEquals("other", Lines.condition("red!=blue:other|same"));
        assertNull(Lines.condition("red>blue:a|b"));
        assertEquals("a", Lines.condition("filled:a|b"));
        assertEquals("b", Lines.condition(":a|b"));
    }

    @Test
    void durationsAreWrittenByUnit() {
        assertEquals("1d 1h 1m 1s", Lines.duration(90061));
        assertEquals("5h", Lines.duration(18000));
        assertEquals("45s", Lines.duration(45));
        assertEquals("0s", Lines.duration(0));
    }

    @Test
    void containsOperatorsIgnoreCase() {
        assertTrue(Lines.test("bread 16, Netherite_Ingot 1~netherite"));
        assertFalse(Lines.test("bread 16~diamond"));
        assertTrue(Lines.test("bread 16!~diamond"));
        assertEquals("yes", Lines.condition("a, b, c~b:yes|no"));
    }

    @Test
    void testWithoutOperatorChecksForAValue() {
        assertTrue(Lines.test("MyServer"));
        assertFalse(Lines.test(""));
        assertFalse(Lines.test("false"));
        assertFalse(Lines.test("0"));
        assertTrue(Lines.test("7>=7"));
        assertNull(Lines.test("a>b"));
    }

    @Test
    void ifDelaySoundAndLogPrefixesAreRead() {
        Lines.Line line = Lines.parse("[if:{x}>=5][delay:1.5][sound:minecraft:block.bell.use:1:2][to:all]&7Hi", 1000);

        assertEquals("{x}>=5", line.getCondition());
        assertEquals(1.5, line.getDelay());
        assertEquals("minecraft:block.bell.use:1:2", line.getSound());
        assertEquals("all", line.getTo());
        assertEquals("&7Hi", line.getText());
        assertFalse(line.isLog());

        Lines.Line log = Lines.parse("[log]{player} used it", 1000);
        assertTrue(log.isLog());
        assertEquals("{player} used it", log.getText());
        assertEquals(0, Lines.parse("[delay:soon]x", 1000).getDelay());
    }

    @Test
    void evaluateUsesConditionsAndPing() {
        assertEquals("&aWin 80%", Lines.evaluate("{if:{calc:40*2}>=50:&aWin|&cLose} 80%", random));
        assertEquals("42ms {ping:Nobody}", Lines.evaluate("{ping:Steve}ms {ping:Nobody}", random,
                (name, spec) -> name.equals("ping") && spec.equals("Steve") ? "42" : null));
    }

    @Test
    void savePrefixesAreRead() {
        Lines.Line global = Lines.parse("[save:brand]{brand}", 1000);
        assertEquals("brand", global.getSave());
        assertFalse(global.isPerPlayer());
        assertEquals("{brand}", global.getText());

        Lines.Line player = Lines.parse("[to:all][save-player:color]red", 1000);
        assertEquals("color", player.getSave());
        assertTrue(player.isPerPlayer());
        assertEquals("all", player.getTo());

        assertNull(Lines.parse("[brand]text", 1000).getSave());
    }

    @Test
    void savedValuesComeFromTheExternalResolver() {
        String text = Lines.evaluate("[brand]{saved:brand} {saved-player:color} {calc:{saved:wins}+1}", random,
                (name, spec) -> name.equals("saved") ? (spec.equals("brand") ? "MyServer" : "4") : "blue");
        assertEquals("[brand]MyServer blue 5", text);
    }

    @Test
    void applyReplacesValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("player", "Steve");
        values.put("amount", "$5");

        assertEquals("Steve paid $5", Lines.apply("{player} paid {amount}", values));
    }
}
