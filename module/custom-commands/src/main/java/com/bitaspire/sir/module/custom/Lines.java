package com.bitaspire.sir.module.custom;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
class Lines {

    private final Pattern PREFIX = Pattern.compile("^\\[(to|repeat|save-player|save|if|delay|sound|log|for)(?::([^]]+))?]", Pattern.CASE_INSENSITIVE);
    private final Pattern FUNCTION = Pattern.compile("\\{(random|calc|pick|if|ping|saved-player|saved):([^{}]+)}", Pattern.CASE_INSENSITIVE);
    private final Pattern CONDITION = Pattern.compile("(.*?)(>=|<=|!=|!~|=|~|>|<)(.*)");
    private final Pattern RANGE = Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s*-\\s*(-?\\d+(?:\\.\\d+)?)");

    @Getter
    @AllArgsConstructor
    static final class Line {
        // Who receives or runs the line; null means the sender.
        @Nullable
        private final String to;
        private final int repeat;
        private final String text;
        // Key the line saves its text under instead of sending or running it; null for none.
        @Nullable
        private final String save;
        // true: saved for each receiver, false: once for the whole server.
        private final boolean perPlayer;
        // Checked for each receiver before the line is used; null for none.
        @Nullable
        private final String condition;
        // Seconds to wait before the line is used.
        private final double delay;
        // Played to each receiver; null for none.
        @Nullable
        private final String sound;
        // true: written to the console instead of being sent or run.
        private final boolean log;
        // Random value whose rolls the line is used once for each; null for none.
        @Nullable
        private final String each;
    }

    // Reads the module's own prefixes from the start of the line, in any order.
    Line parse(String raw, int maxRepeat) {
        String to = null, text = raw, save = null, condition = null, sound = null, each = null;
        int repeat = 1;
        double delay = 0;
        boolean perPlayer = false, log = false;

        Matcher matcher;
        while ((matcher = PREFIX.matcher(text)).find()) {
            String value = matcher.group(2) == null ? "" : matcher.group(2).trim();

            String prefix = matcher.group(1).toLowerCase(Locale.ENGLISH);
            if (prefix.equals("to")) to = value;
            else if (prefix.startsWith("save")) {
                save = value;
                perPlayer = prefix.equals("save-player");
            }
            else if (prefix.equals("if")) condition = value;
            else if (prefix.equals("sound")) sound = value;
            else if (prefix.equals("log")) log = true;
            else if (prefix.equals("for")) each = value;
            else if (prefix.equals("delay")) try {
                delay = Math.max(0, Double.parseDouble(value));
            } catch (NumberFormatException ignored) {}
            else try {
                repeat = Math.max(1, Math.min(maxRepeat, Integer.parseInt(value)));
            } catch (NumberFormatException ignored) {}

            text = text.substring(matcher.end());
        }

        return new Line(to, repeat, text, save, perPlayer, condition, delay, sound, log, each);
    }

    // 90061 is "1d 1h 1m 1s"; units at 0 are left out.
    String duration(long seconds) {
        long[] sizes = {86400, 3600, 60, 1};
        String[] units = {"d", "h", "m", "s"};

        List<String> parts = new ArrayList<>();
        for (int i = 0; i < sizes.length; i++) {
            long amount = seconds / sizes[i];
            seconds %= sizes[i];
            if (amount > 0) parts.add(amount + units[i]);
        }
        return parts.isEmpty() ? "0s" : String.join(" ", parts);
    }

    String apply(String text, Map<String, String> values) {
        for (Map.Entry<String, String> entry : values.entrySet())
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        return text;
    }

    private int decimals(String number) {
        int dot = number.indexOf('.');
        return dot < 0 ? 0 : number.length() - dot - 1;
    }

    // "a|b|c" picks one value, "1-10" a whole number and "0.5-2.25" a number with as many decimals as the bounds.
    @Nullable
    String random(String spec, Random random) {
        if (spec.contains("|")) {
            String[] values = spec.split("\\|", -1);
            return values[random.nextInt(values.length)].trim();
        }

        Matcher matcher = RANGE.matcher(spec.trim());
        if (!matcher.matches()) return null;

        String first = matcher.group(1), second = matcher.group(2);
        BigDecimal a = new BigDecimal(first), b = new BigDecimal(second);
        if (a.compareTo(b) > 0) {
            BigDecimal swap = a;
            a = b;
            b = swap;
        }

        int scale = Math.max(decimals(first), decimals(second));
        if (scale == 0) {
            long min = a.longValue(), span = b.longValue() - min + 1;
            return String.valueOf(min + (long) Math.floor(random.nextDouble() * span));
        }

        BigDecimal value = a.add(b.subtract(a).multiply(BigDecimal.valueOf(random.nextDouble())));
        return value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }

    // "2:a|b|c" gives the 2nd value. An index out of range or not a whole number gives null.
    @Nullable
    String pick(String spec) {
        int colon = spec.indexOf(':');
        if (colon < 0) return null;

        String[] values = spec.substring(colon + 1).split("\\|", -1);
        try {
            int index = Integer.parseInt(spec.substring(0, colon).trim());
            return index < 1 || index > values.length ? null : values[index - 1].trim();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // + - * / % and parentheses. An invalid expression or a division by zero gives null.
    @Nullable
    String calc(String expression) {
        try {
            Calc calc = new Calc(expression.replace(" ", ""));
            BigDecimal value = calc.sum();
            if (calc.index != calc.text.length()) return null;

            value = value.stripTrailingZeros();
            return (value.scale() < 0 ? value.setScale(0) : value).toPlainString();
        } catch (ArithmeticException | NumberFormatException e) {
            return null;
        }
    }

    @RequiredArgsConstructor
    private static final class Calc {

        private final String text;
        private int index;

        private boolean eat(char c) {
            if (index >= text.length() || text.charAt(index) != c) return false;
            index++;
            return true;
        }

        BigDecimal sum() {
            BigDecimal value = product();
            while (true) {
                if (eat('+')) value = value.add(product());
                else if (eat('-')) value = value.subtract(product());
                else return value;
            }
        }

        private BigDecimal product() {
            BigDecimal value = factor();
            while (true) {
                if (eat('*')) value = value.multiply(factor());
                else if (eat('/')) value = value.divide(factor(), 10, RoundingMode.HALF_UP);
                else if (eat('%')) value = value.remainder(factor());
                else return value;
            }
        }

        private BigDecimal factor() {
            if (eat('-')) return factor().negate();
            if (eat('(')) {
                BigDecimal value = sum();
                if (!eat(')')) throw new NumberFormatException();
                return value;
            }

            int start = index;
            while (index < text.length() && (Character.isDigit(text.charAt(index)) || text.charAt(index) == '.')) index++;
            return new BigDecimal(text.substring(start, index));
        }
    }

    // "75>=50", "red=red", "a, b~b"... Numbers compare as numbers; anything else only with = and !=, and
    // ~ (contains) and !~ (doesn't contain), ignoring case.
    // Without an operator, true when the value isn't empty, "false" or "0". Null when it can't be compared.
    @Nullable
    Boolean test(String condition) {
        Matcher matcher = CONDITION.matcher(condition);
        if (!matcher.matches()) {
            String value = condition.trim();
            return !value.isEmpty() && !value.equalsIgnoreCase("false") && !value.equals("0");
        }

        String left = matcher.group(1).trim(), operator = matcher.group(2), right = matcher.group(3).trim();
        if (operator.endsWith("~")) {
            boolean contains = left.toLowerCase(Locale.ENGLISH).contains(right.toLowerCase(Locale.ENGLISH));
            return operator.equals("~") == contains;
        }

        try {
            int compared = new BigDecimal(left).compareTo(new BigDecimal(right));
            switch (operator) {
                case ">=": return compared >= 0;
                case "<=": return compared <= 0;
                case ">": return compared > 0;
                case "<": return compared < 0;
                case "=": return compared == 0;
                default: return compared != 0;
            }
        } catch (NumberFormatException e) {
            if (operator.equals("=")) return left.equalsIgnoreCase(right);
            if (operator.equals("!=")) return !left.equalsIgnoreCase(right);
            return null;
        }
    }

    // "{percent}>=50:yes|no" gives yes or no; without '|' the else part is empty.
    @Nullable
    String condition(String spec) {
        int colon = spec.indexOf(':');
        if (colon < 0) return null;

        Boolean result = test(spec.substring(0, colon));
        if (result == null) return null;

        String[] branches = spec.substring(colon + 1).split("\\|", 2);
        return result ? branches[0] : branches.length == 2 ? branches[1] : "";
    }

    String evaluate(String text, Random random) {
        return evaluate(text, random, (name, spec) -> null);
    }

    // Resolves every {random:...}, {calc:...}, {pick:...} and {if:...}, innermost first, so they can be
    // nested: {calc:{random:1-6}+{random:1-6}}. {ping:...}, {saved:...} and {saved-player:...} need the
    // server, so 'external' gets their name and spec. Invalid ones are left as written.
    String evaluate(String text, Random random, BiFunction<String, String, String> external) {
        while (true) {
            Matcher matcher = FUNCTION.matcher(text);
            StringBuffer buffer = new StringBuffer();

            while (matcher.find()) {
                String spec = matcher.group(2), value;
                switch (matcher.group(1).toLowerCase(Locale.ENGLISH)) {
                    case "random":
                        value = random(spec, random);
                        break;
                    case "calc":
                        value = calc(spec);
                        break;
                    case "if":
                        value = condition(spec);
                        break;
                    case "pick":
                        value = pick(spec);
                        break;
                    default:
                        value = external.apply(matcher.group(1).toLowerCase(Locale.ENGLISH), spec.trim());
                }
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(value == null ? matcher.group() : value));
            }

            matcher.appendTail(buffer);
            if (buffer.toString().equals(text)) return text;
            text = buffer.toString();
        }
    }
}
