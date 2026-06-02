package com.bitaspire.sir.channel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Defines the visual presentation of messages sent through a {@link ChatChannel}.
 *
 * <p> Includes text decoration (tag, prefix, suffix, color), allowed color types,
 * the overall message format string, and optional interactive components (click, hover).
 */
public interface Style {

    /**
     * Returns the MiniMessage tag used to scope this channel's styling (e.g., {@code <channel>}).
     *
     * @return the tag string, or {@code null} if not set.
     */
    @Nullable
    String getTag();

    /**
     * Returns the channel prefix prepended to messages (distinct from the player prefix).
     *
     * @return the prefix, or {@code null} if not set.
     */
    @Nullable
    String getPrefix();

    /**
     * Returns the channel suffix appended to messages.
     *
     * @return the suffix, or {@code null} if not set.
     */
    @Nullable
    String getSuffix();

    /**
     * Returns the default color applied to the message body.
     *
     * @return the color string, or {@code null} if not set.
     */
    @Nullable
    String getColor();

    /**
     * Returns whether players may use standard color codes (e.g., {@code &a}).
     *
     * @return {@code true} if normal colors are allowed.
     */
    boolean allowsNormalColors();

    /**
     * Returns whether players may use special formatting codes (bold, italic, etc.).
     *
     * @return {@code true} if special colors are allowed.
     */
    boolean allowsSpecialColors();

    /**
     * Returns whether players may use RGB/hex color codes.
     *
     * @return {@code true} if RGB colors are allowed.
     */
    boolean allowsRgbColors();

    /**
     * Returns the click action attached to messages in this channel, if any.
     *
     * @return the click action, or {@code null} if not configured.
     */
    @Nullable
    Click getClick();

    /**
     * Returns the hover tooltip lines shown when a player hovers over messages.
     *
     * @return the hover lines, or {@code null} if not configured.
     */
    @Nullable
    List<String> getHover();

    /**
     * Returns the message format string (supports placeholders).
     *
     * @return the format.
     */
    @NotNull
    String getFormat();

    /**
     * Sets the message format string.
     *
     * @param format the new format string.
     */
    void setFormat(@NotNull String format);

    /**
     * Returns whether this channel sends messages without requiring a Bukkit chat event.
     *
     * <p> A channel is considered eventless when neither a click action nor hover tooltip is configured.
     *
     * @return {@code true} if no interactive components are present.
     */
    default boolean isChatEventless() {
        List<String> hover = getHover();
        Click click = getClick();

        return (click == null || click.isEmpty()) && (hover == null || hover.isEmpty());
    }
}
