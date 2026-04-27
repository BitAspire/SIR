package com.bitaspire.sir.channel;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Access {

    boolean isDefault();

    @NotNull
    List<String> getPrefixes();

    @NotNull
    List<String> getCommands();

    boolean shouldStripPrefix();

    @Nullable
    default String getMatchingPrefix(String message) {
        if (StringUtils.isBlank(message)) return null;

        return getPrefixes().stream()
                .filter(StringUtils::isNotBlank)
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .filter(message::startsWith)
                .findFirst()
                .orElse(null);
    }

    default boolean isEmpty() {
        return getPrefixes().isEmpty() && getCommands().isEmpty();
    }

    default boolean isConfigured() {
        return isDefault() || !isEmpty();
    }
}
