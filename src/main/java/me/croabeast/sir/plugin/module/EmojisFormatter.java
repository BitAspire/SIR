package me.croabeast.sir.plugin.module;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.croabeast.prismatic.PrismaticAPI;
import me.croabeast.sir.api.file.PermissibleUnit;
import me.croabeast.sir.plugin.FileData;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class EmojisFormatter extends SIRModule implements PlayerFormatter<Object> {

    private final Set<Emoji> emojis = new LinkedHashSet<>();

    EmojisFormatter() {
        super(Key.EMOJIS);
    }

    @Override
    public boolean register() {
        emojis.clear();
        FileData.Module.Chat.EMOJIS.getFile()
                .getSections("emojis")
                .forEach((k, s) ->
                        emojis.add(new Emoji(s)));

        return true;
    }

    @Override
    public boolean unregister() {
        return false;
    }

    @Override
    public String format(Player player, String string, Object reference) {
        return format(player, string);
    }

    @Override
    public String format(Player player, String string) {
        if (!isEnabled() || emojis.isEmpty())
            return string;

        for (Emoji e : emojis)
            string = e.parse(player, string);

        return string;
    }

    static class Emoji implements PermissibleUnit {

        private final ConfigurationSection section;

        private final String key, value;
        private final Checks checks;

        Emoji(ConfigurationSection section) {
            this.section = section;

            key = section.getString("key");
            value = section.getString("value");

            Checks checks = new Checks(false, false, true);
            try {
                checks = new Checks(section);
            } catch (NullPointerException ignored) {}

            this.checks = checks;
        }

        @NotNull
        public ConfigurationSection getSection() {
            return section;
        }

        String convertValue(String line) {
            return (value == null ? "" : value) + PrismaticAPI.getLastColor(line);
        }

        Matcher getMatcher(String line, boolean add) {
            if (key == null) return null;

            String inCase = !checks.isSensitive() ? "(?i)" : "",
                    k = checks.isRegex() ? key : Pattern.quote(key);

            if (add) k = "^" + k + "$";
            return Pattern.compile(inCase + k).matcher(line);
        }

        String parse(Player player, String line) {
            if (StringUtils.isBlank(line) || key == null) return line;

            if (player != null && !hasPerm(player)) return line;

            if (checks.isWord()) {
                StringBuilder builder = new StringBuilder();
                String[] words = line.split(" ");

                for (int i = 0; i < words.length; i++) {
                    String w = words[i];
                    Matcher match = getMatcher(PrismaticAPI.stripAll(w), true);

                    if (match == null) {
                        if (i > 0) builder.append(" ");
                        builder.append(w);
                        continue;
                    }

                    if (match.find()) {
                        if (i > 0) builder.append(" ");
                        builder.append(convertValue(line));
                        continue;
                    }

                    if (i > 0) builder.append(" ");
                    builder.append(w);
                }

                return builder.toString();
            }

            Matcher match = getMatcher(line, false);
            if (match == null) return line;

            while (match.find())
                line = line.replace(match.group(), convertValue(line));

            return line;
        }

        @Override
        public String toString() {
            return "Emoji{" +
                    "perm='" + getPermission() + '\'' +
                    ", key='" + key + '\'' +
                    ", value='" + value + '\'' +
                    ", checks=" + checks + '}';
        }
    }

    @RequiredArgsConstructor
    @Getter
    static class Checks {

        private final boolean regex, isWord, sensitive;

        public Checks(ConfigurationSection id) {
            if (id == null) throw new NullPointerException();

            id = id.getConfigurationSection("checks");
            if (id == null) throw new NullPointerException();

            regex = id.getBoolean("is-regex");
            isWord = id.getBoolean("is-word");
            sensitive = id.getBoolean("case-sensitive");
        }

        @Override
        public String toString() {
            return '{' + "regex=" + regex + ", isWord=" + isWord + ", sensitive=" + sensitive + '}';
        }
    }
}
