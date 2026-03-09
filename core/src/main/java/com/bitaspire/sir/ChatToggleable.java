package com.bitaspire.sir;

import com.bitaspire.sir.command.SettingsService;
import com.bitaspire.sir.user.SIRUser;
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
