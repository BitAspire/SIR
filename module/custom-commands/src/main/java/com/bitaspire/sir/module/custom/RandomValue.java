package com.bitaspire.sir.module.custom;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// One value of 'random-values'. A text spec rolls like {random:...}. A list, or a section with 'entries',
// picks entries by their [weight:N] and [chance:N%], 'times' times. Every roll shows its text and keeps
// its [value:...], the text itself when it has none.
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class RandomValue {

    private static final Pattern PREFIX = Pattern.compile("^\\[(weight|chance|value):([^]]*)]", Pattern.CASE_INSENSITIVE);

    private final String key;
    @Nullable
    private final String spec;
    private final List<String> shown, values;
    private final List<Double> weights;
    private final String times, separator;
    private final boolean unique;

    @Nullable
    static RandomValue of(String key, ConfigurationSection parent, String label, List<String> warnings) {
        if (!parent.isList(key) && !parent.isConfigurationSection(key)) {
            String spec = parent.getString(key, "");
            return new RandomValue(key, spec, null, null, null, "1", ", ", false);
        }

        ConfigurationSection section = parent.getConfigurationSection(key);
        List<String> raw = section == null ? parent.getStringList(key) : section.getStringList("entries");

        List<String> shown = new ArrayList<>(), values = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        if (!pool(raw, shown, values, weights, label, warnings)) return null;

        if (section == null) return new RandomValue(key, null, shown, values, weights, "1", ", ", false);
        return new RandomValue(key, null, shown, values, weights,
                section.getString("times", "1"), section.getString("separator", ", "), section.getBoolean("unique"));
    }

    // Reads the entries into their text, value and weight out of 100. An entry with [chance:N%] gets
    // exactly N%; the rest of 100 is shared by the others by their [weight:N], 1 by default. When every
    // entry has a chance and they add up to less than 100, the rest picks nothing. False when nothing
    // can be picked.
    static boolean pool(List<String> raw, List<String> shown, List<String> values, List<Double> weights,
                        String label, List<String> warnings) {
        if (raw.isEmpty()) {
            warnings.add(label + " has no entries");
            return false;
        }

        List<Double> chances = new ArrayList<>();
        double chanceSum = 0, weightSum = 0;

        for (String entry : raw) {
            double weight = 1, chance = -1;
            String value = null;

            Matcher matcher;
            while ((matcher = PREFIX.matcher(entry)).find()) {
                String prefix = matcher.group(1).toLowerCase(Locale.ENGLISH), argument = matcher.group(2).trim();
                entry = entry.substring(matcher.end());

                if (prefix.equals("value")) {
                    value = argument;
                    continue;
                }

                try {
                    double number = Math.max(0, Double.parseDouble(argument.replace("%", "")));
                    if (prefix.equals("chance")) chance = number;
                    else weight = number;
                } catch (NumberFormatException e) {
                    warnings.add(label + " has an invalid [" + prefix + ":" + argument + "]");
                }
            }

            shown.add(entry);
            values.add(value == null ? entry : value);
            weights.add(weight);
            chances.add(chance);

            if (chance >= 0) chanceSum += chance;
            else weightSum += weight;
        }

        double scale = 1;
        if (chanceSum > 100) {
            warnings.add(label + " has chances adding up to " + chanceSum + "%, so they are scaled down to 100%");
            scale = 100 / chanceSum;
            chanceSum = 100;
        }

        double rest = 100 - chanceSum;
        for (int i = 0; i < shown.size(); i++) {
            double chance = chances.get(i);
            weights.set(i, chance >= 0 ? chance * scale : weightSum > 0 ? rest * weights.get(i) / weightSum : 0);
        }

        if (weights.stream().noneMatch(weight -> weight > 0)) {
            warnings.add(label + " has no entry with a weight or chance above 0");
            return false;
        }

        if (weightSum <= 0 && rest > 0) {
            shown.add("");
            values.add("");
            weights.add(rest);
        }
        return true;
    }

    // Index picked by weight, -1 when every weight is 0 or less.
    private static int pick(List<Double> weights, Random random) {
        double total = weights.stream().mapToDouble(w -> Math.max(0, w)).sum();
        if (total <= 0) return -1;

        double roll = random.nextDouble() * total;
        int last = -1;
        for (int i = 0; i < weights.size(); i++) {
            if (weights.get(i) <= 0) continue;
            last = i;
            if ((roll -= weights.get(i)) < 0) return i;
        }
        return last;
    }

    // Puts {key}, {key.value}, {key.sum}, {key.count}, {key.N} and {key.N.value} for one use. 'times' can
    // use the values already in 'into'; it is kept between 0 and 'maxTimes'. With 'unique', an entry is
    // picked once at most, so there are fewer rolls when the entries run out. The rest of the chances
    // picks nothing, so it isn't a roll.
    void roll(Map<String, String> into, Random random, int maxTimes) {
        int count = 1;
        try {
            count = Integer.parseInt(Lines.apply(times, into).trim());
        } catch (NumberFormatException ignored) {}
        count = Math.max(0, Math.min(maxTimes, count));

        List<String> texts = new ArrayList<>(), rolled = new ArrayList<>();
        List<Double> pool = weights == null ? null : new ArrayList<>(weights);

        for (int i = 0; i < count; i++) {
            if (spec != null) {
                String text = Lines.random(spec, random);
                texts.add(text == null ? spec : text);
                rolled.add(text == null ? spec : text);
                continue;
            }

            int index = pick(pool, random);
            if (index < 0) break;
            if (unique) pool.set(index, 0D);

            if (shown.get(index).isEmpty() && values.get(index).isEmpty()) continue;
            texts.add(shown.get(index));
            rolled.add(values.get(index));
        }

        BigDecimal sum = null;
        for (String value : rolled)
            try {
                BigDecimal number = new BigDecimal(value.trim());
                sum = sum == null ? number : sum.add(number);
            } catch (NumberFormatException ignored) {}

        into.put(key, String.join(separator, texts));
        into.put(key + ".value", String.join(separator, rolled));
        into.put(key + ".sum", sum == null ? "" : sum.stripTrailingZeros().toPlainString());
        into.put(key + ".count", String.valueOf(texts.size()));

        for (int i = 0; i < texts.size(); i++) {
            into.put(key + "." + (i + 1), texts.get(i));
            into.put(key + "." + (i + 1) + ".value", rolled.get(i));
        }
    }
}
