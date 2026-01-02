package me.croabeast.sir.command.settings;

import me.croabeast.sir.ChatToggleable;
import me.croabeast.sir.PAPIExpansion;
import me.croabeast.sir.user.SIRUser;
import org.apache.commons.lang.StringUtils;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

final class Expansion {

    private final PAPIExpansion expansion;

    Expansion(SettingsProvider provider) {
        expansion = new PAPIExpansion("sir_toggle") {
            @Override
            public String onRequest(OfflinePlayer off, @NotNull String params) {
                if (off == null || StringUtils.isBlank(params)) return null;

                SIRUser user = provider.getApi().getUserManager().getUser(off);
                if (user == null) return null;

                ChatToggleable toggleable = provider.findToggleable(SettingsProvider.Category.MODULES, params);
                if (toggleable == null)
                    toggleable = provider.findToggleable(SettingsProvider.Category.COMMANDS, params);

                return toggleable == null ? null : String.valueOf(provider.isToggled(user, toggleable));
            }
        };
    }

    void register() {
        expansion.register();
    }

    void unregister() {
        expansion.unregister();
    }
}
