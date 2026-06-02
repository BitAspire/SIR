package com.bitaspire.sir;

import com.bitaspire.sir.user.SIRUser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Strategy interface for formatting strings in the context of a specific user.
 *
 * <p> Implementations apply user-specific transformations (placeholders, colors, etc.)
 * to a raw string using an optional typed reference value.
 *
 * @param <T> the type of the optional reference object passed to the formatter.
 */
public interface UserFormatter<T> {

    /**
     * Formats {@code string} in the context of the given user and reference.
     *
     * @param user the user context.
     * @param string the raw string to format.
     * @param reference an optional reference value used during formatting, or {@code null}.
     * @return the formatted string.
     */
    @NotNull
    String format(SIRUser user, String string, T reference);

    /**
     * Formats {@code string} in the context of the given user with no reference.
     *
     * @param user the user context.
     * @param string the raw string to format.
     * @return the formatted string.
     */
    @NotNull
    default String format(SIRUser user, String string) {
        return format(user, string, null);
    }

    /**
     * Formats {@code string} in the context of the given player and reference.
     *
     * @param player the player whose {@link com.bitaspire.sir.user.SIRUser} will be looked up.
     * @param string the raw string to format.
     * @param reference an optional reference value, or {@code null}.
     * @return the formatted string.
     */
    @NotNull
    default String format(Player player, String string, T reference) {
        return format(SIRApi.instance().getUserManager().getUser(player), string, reference);
    }

    /**
     * Formats {@code string} in the context of the given player with no reference.
     *
     * @param player the player whose {@link com.bitaspire.sir.user.SIRUser} will be looked up.
     * @param string the raw string to format.
     * @return the formatted string.
     */
    @NotNull
    default String format(Player player, String string) {
        return format(SIRApi.instance().getUserManager().getUser(player), string, null);
    }
}
