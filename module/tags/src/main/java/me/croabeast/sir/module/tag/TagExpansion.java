package me.croabeast.sir.module.tag;

import me.croabeast.sir.PAPIExpansion;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class TagExpansion extends PAPIExpansion {

    private final Tags tags;

    TagExpansion(Tags tags) {
        super("sir_tag");
        this.tags = tags;
    }

    @Nullable
    @Override
    public String onRequest(OfflinePlayer off, @NotNull String params) {
        Player player = off.getPlayer();
        if (player == null) return null;

        SIRUser user = tags.getApi().getUserManager().getUser(player);
        return user == null ? null : tags.parseTag(user, params);
    }
}
