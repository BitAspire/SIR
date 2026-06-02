package com.bitaspire.sir;

import com.bitaspire.sir.command.SettingsService;
import com.bitaspire.sir.user.SIRUser;
import org.jetbrains.annotations.NotNull;

/**
 * Marks a component whose chat-related behavior can be toggled per user.
 *
 * <p> Typically implemented by extensions or modules that expose a settings toggle
 * via the {@code /settings} command.
 */
public interface ChatToggleable {

    /**
     * Returns the unique key used to store the toggle state for this component.
     *
     * <p> Defaults to the extension name if this is a {@link SIRExtension},
     * or to the simple class name otherwise.
     *
     * @return the toggle key.
     */
    @NotNull
    default String getKey() {
        return this instanceof SIRExtension ? ((SIRExtension<?>) this).getName() : getClass().getSimpleName();
    }

    /**
     * Returns whether this component is toggled on for the given user.
     *
     * <p> Returns {@code true} if no {@link SettingsService} is available (opt-in by default).
     *
     * @param user the user to check.
     * @return {@code true} if toggled on.
     */
    default boolean isToggled(SIRUser user) {
        SettingsService service = SIRApi.instance().getCommandManager().getSettingsService();
        return service == null || service.isToggled(user, this);
    }
}
