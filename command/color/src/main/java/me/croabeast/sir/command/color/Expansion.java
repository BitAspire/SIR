package me.croabeast.sir.command.color;

import me.croabeast.sir.PAPIExpansion;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.user.ColorData;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

final class Expansion {

    private final SIRApi api = SIRApi.instance();
    private final PAPIExpansion expansion;

    Expansion() {
        expansion = new PAPIExpansion("sir_color") {
            @Override
            public String onRequest(OfflinePlayer off, @NotNull String params) {
                Player player = off.getPlayer();
                if (player == null) {
                    SIRUser user = api.getUserManager().getUser(off);
                    if (user == null) return null;

                    ColorData data = user.getColorData();
                    if (params.matches("(?i)start"))
                        return data.getStart();
                    else if (params.matches("(?i)end"))
                        return data.getEnd();
                    return null;
                }

                SIRUser user = api.getUserManager().getUser(player);
                if (user == null) return null;

                ColorData data = user.getColorData();
                if (params.matches("(?i)start"))
                    return data.getStart();
                else if (params.matches("(?i)end"))
                    return data.getEnd();

                return null;
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
