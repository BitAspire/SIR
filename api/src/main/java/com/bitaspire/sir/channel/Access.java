package com.bitaspire.sir.channel;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Defines how a {@link ChatChannel} is accessed by players.
 *
 * <p> A channel can be entered via chat message prefixes (e.g., {@code !message}),
 * dedicated commands, or it may be the server default channel.
 */
public interface Access {

    /**
     * Returns whether this is the default channel that receives unfiltered chat messages.
     *
     * @return {@code true} if this is the default channel.
     */
    boolean isDefault();

    /**
     * Returns the list of message prefixes that route chat into this channel.
     *
     * @return the prefix list; may be empty.
     */
    @NotNull
    List<String> getPrefixes();

    /**
     * Returns the list of command names that switch a player into this channel.
     *
     * @return the command list; may be empty.
     */
    @NotNull
    List<String> getCommands();

    /**
     * Returns whether the matching prefix should be stripped from the message before delivery.
     *
     * @return {@code true} if the prefix is removed.
     */
    boolean shouldStripPrefix();

    /**
     * Finds the longest prefix from {@link #getPrefixes()} that the given message starts with.
     *
     * @param message the raw chat message.
     * @return the matching prefix, or {@code null} if none matches or the message is blank.
     */
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

    /**
     * Returns {@code true} if no prefixes or commands are defined.
     *
     * @return {@code true} if both lists are empty.
     */
    default boolean isEmpty() {
        return getPrefixes().isEmpty() && getCommands().isEmpty();
    }

    /**
     * Returns whether this access definition is configured enough to route messages.
     *
     * <p> A channel is considered configured if it is the default or has at least one
     * prefix or command defined.
     *
     * @return {@code true} if configured.
     */
    default boolean isConfigured() {
        return isDefault() || !isEmpty();
    }
}
