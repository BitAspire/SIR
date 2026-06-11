package com.bitaspire.sir.channel;

import com.bitaspire.sir.PermissibleUnit;
import com.bitaspire.sir.user.SIRUser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Represents a configurable chat channel in the SIR channels module.
 *
 * <p> A channel groups together access rules ({@link Access}), audience scope ({@link Audience}),
 * display style ({@link Style}), and logging settings ({@link Logging}). Channels can be
 * hierarchically linked via parent/sub-channel references.
 */
public interface ChatChannel extends PermissibleUnit {

    /**
     * Returns whether this channel broadcasts to all players (global), as opposed to a local radius.
     *
     * @return {@code true} if global.
     */
    boolean isGlobal();

    /**
     * Returns whether this channel is local (radius-based), i.e., not global.
     *
     * @return {@code true} if local.
     */
    default boolean isLocal() {
        return !isGlobal();
    }

    /**
     * Returns the parent channel that this channel inherits settings from, if any.
     *
     * @return the parent channel, or {@code null} if none.
     */
    @Nullable
    ChatChannel getParent();

    /**
     * Returns a sub-channel nested under this channel, if configured.
     *
     * @return the sub-channel, or {@code null} if none.
     */
    @Nullable
    ChatChannel getSubChannel();

    /**
     * Returns the access configuration controlling how players enter this channel.
     *
     * @return the access rules.
     */
    @NotNull
    Access getAccess();

    /**
     * Returns the audience configuration controlling who receives messages in this channel.
     *
     * @return the audience rules.
     */
    @NotNull
    Audience getAudience();

    /**
     * Returns the display style applied to messages in this channel.
     *
     * @return the style settings.
     */
    @NotNull
    Style getStyle();

    /**
     * Returns the logging configuration for this channel.
     *
     * @return the logging settings.
     */
    @NotNull
    Logging getLogging();

    /**
     * Returns whether this channel sends messages without firing a Bukkit chat event.
     *
     * @return {@code true} if no chat event is fired.
     */
    default boolean isChatEventless() {
        return getStyle().isChatEventless();
    }

    /**
     * Returns whether this channel behaves as the server default (eventless and no radius).
     *
     * @return {@code true} if this is the default channel.
     */
    default boolean isDefault() {
        return isChatEventless() && getAudience().getRadius() <= 0;
    }

    /**
     * Returns whether this channel can be accessed via a prefix or command (non-passive).
     *
     * @return {@code true} if explicitly accessible.
     */
    default boolean isLocalAccessible() {
        return getAccess().isConfigured();
    }

    /**
     * Returns whether the given message starts with a prefix that routes to this channel.
     *
     * @param message the raw chat message.
     * @return {@code true} if a matching prefix is found.
     */
    default boolean isAccessibleByPrefix(String message) {
        return getAccess().getMatchingPrefix(message) != null;
    }

    /**
     * Returns whether the given command name routes to this channel.
     *
     * @param command the command name (without leading slash).
     * @return {@code true} if the command is registered for this channel.
     */
    default boolean isAccessibleByCommand(String command) {
        return getAccess().getCommands().stream()
                .anyMatch(s -> s.equalsIgnoreCase(command));
    }

    /**
     * Returns the set of users that will receive a message from the given sender user.
     *
     * @param user the sending user.
     * @return the recipient set.
     */
    @NotNull
    Set<SIRUser> getRecipients(SIRUser user);

    /**
     * Returns the set of users that will receive a message from the given sender player.
     *
     * @param player the sending player.
     * @return the recipient set.
     */
    @NotNull
    Set<SIRUser> getRecipients(Player player);

    /**
     * Returns the placeholder keys defined by this channel's format (e.g., {@code {prefix}}, {@code {name}}).
     *
     * @return the placeholder key array.
     */
    @NotNull
    String[] getChatKeys();

    /**
     * Returns the resolved placeholder values for the given raw message.
     *
     * @param message the raw chat message.
     * @return the resolved value array corresponding to {@link #getChatKeys()}.
     */
    @NotNull
    String[] getChatValues(String message);

    /**
     * Returns the resolved placeholder values for the given raw message and sender context.
     *
     * <p> Implementations may use the sender to resolve user-specific placeholders.
     * The default implementation keeps backwards compatibility with older channel implementations.
     *
     * @param user the sending user, or {@code null} if unavailable.
     * @param message the raw chat message.
     * @return the resolved value array corresponding to {@link #getChatKeys()}.
     */
    @NotNull
    default String[] getChatValues(@Nullable SIRUser user, String message) {
        return getChatValues(message);
    }

    /**
     * Formats a string for {@code target} as parsed by {@code parser}.
     *
     * @param target the player receiving the formatted string (may differ from the sender).
     * @param parser the player whose context (permissions, placeholders) is used during formatting.
     * @param string the raw string to format.
     * @param isChat {@code true} if this is a chat message (enables chat-specific processing).
     * @return the formatted string.
     */
    @NotNull
    String formatString(Player target, Player parser, String string, boolean isChat);

    /**
     * Formats a string for {@code target} as parsed by {@code parser}, treating it as a chat message.
     *
     * @param target the player receiving the formatted string.
     * @param parser the player whose context is used during formatting.
     * @param string the raw string to format.
     * @return the formatted string.
     */
    @NotNull
    default String formatString(Player target, Player parser, String string) {
        return formatString(target, parser, string, true);
    }

    /**
     * Formats a string using the given player as both target and parser.
     *
     * @param player the player used as both recipient and format context.
     * @param string the raw string to format.
     * @param isChat {@code true} if this is a chat message.
     * @return the formatted string.
     */
    @NotNull
    default String formatString(Player player, String string, boolean isChat) {
        return formatString(player, player, string, isChat);
    }

    /**
     * Formats a string using the given player as both target and parser, treating it as a chat message.
     *
     * @param player the player used as both recipient and format context.
     * @param string the raw string to format.
     * @return the formatted string.
     */
    @NotNull
    default String formatString(Player player, String string) {
        return formatString(player, player, string, true);
    }
}
