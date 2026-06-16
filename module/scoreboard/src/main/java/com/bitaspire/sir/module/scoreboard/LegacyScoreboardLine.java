package com.bitaspire.sir.module.scoreboard;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.croabeast.prismatic.PrismaticAPI;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class LegacyScoreboardLine {

    private static final String COLOR_CHAR = "\u00A7";

    static String trim(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;

        String trimmed = value.substring(0, max);
        return trimmed.endsWith(COLOR_CHAR) ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    static Parts split(String value) {
        String text = value == null ? "" : value;
        if (text.length() <= 16) return new Parts(text, "");

        String prefix = trim(text, 16);
        String suffixSource = text.substring(prefix.length());
        String suffix = PrismaticAPI.getEndColor(prefix) + suffixSource;

        return new Parts(prefix, trim(suffix, 16));
    }

    static final class Parts {
        final String prefix;
        final String suffix;

        Parts(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }
    }
}
