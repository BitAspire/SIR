package me.croabeast.sir.module.emoji;

import me.croabeast.sir.PAPIExpansion;
import me.croabeast.sir.module.SIRModule;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class Emojis extends SIRModule {

    Data data;
    PAPIExpansion hook;

    @Override
    public boolean register() {
        data = new Data(this);

        return !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") ||
                (hook = new PAPIExpansion("sir_emoji") {
                    @NotNull
                    public String onRequest(OfflinePlayer off, @NotNull String params) {
                        Emoji emoji = data.emojis.get(params);
                        return emoji != null && emoji.getValue() != null ? emoji.getValue() : "";
                    }
                }).register();
    }

    @Override
    public boolean unregister() {
        return hook == null || hook.unregister();
    }

    @NotNull
    public String parseEmojis(SIRUser user, String string) {
        if (!isEnabled() || data.emojis.isEmpty())
            return string;

        for (Emoji emoji : data.emojis.values())
            string = emoji.parse(user, string);

        return string;
    }

    @NotNull
    public String parseEmojis(Player player, String string) {
        return parseEmojis(getApi().getUserManager().getUser(player), string);
    }
}
