package me.croabeast.sir.module.emoji;

import lombok.Getter;
import me.croabeast.prismatic.PrismaticAPI;
import me.croabeast.sir.PermissibleUnit;
import me.croabeast.sir.user.SIRUser;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
final class Emoji implements PermissibleUnit {

    private static final int REGEX = 0;
    private static final int WORD = 1;
    private static final int SENSITIVE = 2;

    private static final Pattern TOKENIZER = Pattern.compile("\\S+|\\s+");

    private final ConfigurationSection section;

    private final String key, value;
    private final boolean[] checks;

    private final Pattern findPattern;
    private final Pattern exactPattern;

    Emoji(ConfigurationSection section) {
        this.section = section;

        key = section.getString("key");
        value = section.getString("value");

        checks = new boolean[3];

        this.checks[REGEX] = section.getBoolean("checks.is-regex");
        this.checks[WORD] = section.getBoolean("checks.is-word");
        this.checks[SENSITIVE] = section.getBoolean("checks.case-sensitive", true);

        Pattern fp = null, ep = null;
        if (key != null)
            try {
                int flags = isSensitive() ? 0 : (Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                String k = isRegex() ? key : Pattern.quote(key);

                fp = Pattern.compile(k, flags);
                ep = Pattern.compile("^" + k + "$", flags);
            } catch (Exception ignored) {}

        this.findPattern = fp;
        this.exactPattern = ep;
    }

    private boolean isRegex() {
        return checks[REGEX];
    }

    private boolean isWord() {
        return checks[WORD];
    }

    private boolean isSensitive() {
        return checks[SENSITIVE];
    }

    String parse(SIRUser user, String line) {
        if ((StringUtils.isBlank(line) || key == null) ||
                (user != null && !hasPermission(user)))
            return line;

        String replacement = (value == null ? "" : value) + PrismaticAPI.getEndColor(line);

        if (isWord()) {
            if (exactPattern == null) return line;

            Matcher t = TOKENIZER.matcher(line);
            StringBuffer out = new StringBuffer(line.length());

            while (t.find()) {
                String token = t.group();
                if (token.isEmpty() || Character.isWhitespace(token.charAt(0))) {
                    t.appendReplacement(out, Matcher.quoteReplacement(token));
                    continue;
                }

                t.appendReplacement(out, Matcher.quoteReplacement(
                        !exactPattern.matcher(PrismaticAPI.stripAll(token)).matches() ?
                                token :
                                replacement
                ));
            }

            t.appendTail(out);
            return out.toString();
        }

        if (findPattern == null) return line;

        Matcher m = findPattern.matcher(line);
        if (!m.find()) return line;

        StringBuffer out = new StringBuffer(line.length());
        do {
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        } while (m.find());

        m.appendTail(out);
        return out.toString();
    }
}
