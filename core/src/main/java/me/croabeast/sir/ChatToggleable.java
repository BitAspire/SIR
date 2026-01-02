package me.croabeast.sir;

import me.croabeast.sir.command.SettingsService;
import me.croabeast.sir.user.SIRUser;
import org.jetbrains.annotations.NotNull;

public interface ChatToggleable {

    @NotNull
    default String getKey() {
        return this instanceof SIRExtension ? ((SIRExtension) this).getName() : getClass().getSimpleName();
    }

    default boolean isToggled(SIRUser user) {
        SettingsService service = SIRApi.instance().getCommandManager().getSettingsService();
        return service == null || service.isToggled(user, this);
    }
}
