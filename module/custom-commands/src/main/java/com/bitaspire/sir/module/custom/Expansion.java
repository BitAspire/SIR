package com.bitaspire.sir.module.custom;

import com.bitaspire.sir.PAPIExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

final class Expansion extends PAPIExpansion {

    private final CustomCommands main;

    Expansion(CustomCommands main) {
        super("sir-custom");
        this.main = main;
    }

    @Override
    public String onRequest(OfflinePlayer off, @NotNull String params) {
        if (params.startsWith("saved_"))
            return main.data.get(null, params.substring(6));

        if (params.startsWith("player_") && off != null)
            return main.data.get(off.getUniqueId(), params.substring(7));

        return null;
    }
}
