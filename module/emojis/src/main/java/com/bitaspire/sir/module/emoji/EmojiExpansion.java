package com.bitaspire.sir.module.emoji;

import com.bitaspire.sir.PAPIExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

final class EmojiExpansion extends PAPIExpansion {

    private final Data data;

    EmojiExpansion(Data data) {
        super("sir_emoji");
        this.data = data;
    }

    @NotNull
    @Override
    public String onRequest(OfflinePlayer off, @NotNull String params) {
        Emoji emoji = data.emojis.get(params);
        return emoji != null && emoji.getValue() != null ? emoji.getValue() : "";
    }
}
