package com.bitaspire.sir.user;

import org.jetbrains.annotations.Nullable;

/**
 * Represents the nickname data associated with a {@link SIRUser}.
 *
 * <p> Nicknames are displayed in place of the real player name in chat and other
 * contexts where SIR applies formatting.
 */
public interface NickData {

    /**
     * Returns the current nickname, or {@code null} if none is set.
     *
     * @return the nickname, or {@code null}.
     */
    @Nullable
    String getNick();

    /**
     * Returns whether a non-blank nickname is currently set.
     *
     * @return {@code true} if a nickname is active.
     */
    default boolean hasNick() {
        String nick = getNick();
        return nick != null && !nick.trim().isEmpty();
    }

    /**
     * Sets the nickname for this user.
     *
     * @param nick the new nickname, or {@code null} to clear it.
     */
    void setNick(@Nullable String nick);

    /**
     * Clears the nickname, reverting to the player's real name.
     */
    void resetNick();
}
