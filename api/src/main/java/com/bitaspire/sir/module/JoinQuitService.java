package com.bitaspire.sir.module;

import com.bitaspire.sir.user.SIRUser;

/**
 * Handles join and quit message display for the join-quit module.
 *
 * <p> Implementations apply cooldowns and trigger the appropriate message sequences
 * when a player connects to or disconnects from the server.
 */
public interface JoinQuitService {

    /**
     * Returns whether the given user is on cooldown for a join or quit message.
     *
     * @param user the user to check.
     * @param join {@code true} to check the join cooldown, {@code false} for the quit cooldown.
     * @return {@code true} if on cooldown.
     */
    boolean isOnCooldown(SIRUser user, boolean join);

    /**
     * Displays the join or quit message for the given user.
     *
     * @param user the user who is joining or quitting.
     * @param join {@code true} to show the join message, {@code false} for the quit message.
     */
    void display(SIRUser user, boolean join);
}
