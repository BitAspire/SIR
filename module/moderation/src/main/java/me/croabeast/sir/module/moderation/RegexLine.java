package me.croabeast.sir.module.moderation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RegexLine {

    private final Pattern pattern;

    RegexLine(String line) {
        Pattern regex = Pattern.compile("(?i)\\[regex] *");
        Matcher matcher = regex.matcher(line);

        pattern = Pattern.compile(matcher.find() ?
                line.replace(matcher.group(), "") : Pattern.quote(line));
    }

    Matcher matcher(String string) {
        return pattern.matcher(string);
    }
}
