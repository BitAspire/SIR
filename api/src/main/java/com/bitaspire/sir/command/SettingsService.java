package com.bitaspire.sir.command;

import com.bitaspire.sir.ChatToggleable;
import com.bitaspire.sir.user.SIRUser;

/**
 * Persists and queries per-user toggle states for {@link ChatToggleable} components.
 *
 * <p> Typically backed by a user data store (e.g., JSON file). Used by the
 * {@code /settings} command to let players enable or disable specific features.
 */
public interface SettingsService {

    /**
     * Returns whether the given toggleable is enabled for the user.
     *
     * @param user the user to query.
     * @param toggleable the component whose state is queried.
     * @return {@code true} if toggled on.
     */
    boolean isToggled(SIRUser user, ChatToggleable toggleable);

    /**
     * Updates the toggle state of the given toggleable for the user.
     *
     * @param user the user to update.
     * @param toggleable the component whose state is changed.
     * @param enabled the new toggle state.
     */
    void setToggle(SIRUser user, ChatToggleable toggleable, boolean enabled);
}
