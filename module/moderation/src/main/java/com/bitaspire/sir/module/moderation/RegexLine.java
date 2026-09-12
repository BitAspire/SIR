package com.bitaspire.sir.module.moderation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RegexLine {

    private static final Pattern REGEX_PREFIX = Pattern.compile("(?i)\\[regex] *");

    private final Pattern pattern;

    RegexLine(String line) {
        Matcher matcher = REGEX_PREFIX.matcher(line);

        pattern = Pattern.compile(matcher.find() ?
                line.replace(matcher.group(), "") : Pattern.quote(line));
    }

    Matcher matcher(String string) {
        return pattern.matcher(string);
    }
}
