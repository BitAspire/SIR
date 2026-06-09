package com.bitaspire.sir.module.motd;

import lombok.Getter;
import lombok.SneakyThrows;
import com.bitaspire.sir.file.ExtensionFile;

@Getter
final class Config {

    private final boolean alwaysLoadDefaultIcon, randomMotd;

    private String maxPlayersType = "DEFAULT";
    private int maxPlayersCount = 69420;
    private int centerWidth = 130;

    private String serverIconUsage = "SINGLE";
    private String serverIconImage = "server-icon.png";

    @SneakyThrows
    Config(MOTD main) {
        ExtensionFile file = new ExtensionFile(main, "config", true);

        alwaysLoadDefaultIcon = file.get("always-load-default-icon", true);

        maxPlayersType = file.get("max-players.type", maxPlayersType);
        maxPlayersCount = file.getConfiguration().getInt("max-players.count", maxPlayersCount);
        centerWidth = file.getConfiguration().getInt("center-width", centerWidth);

        serverIconUsage = file.get("server-icon.usage", serverIconUsage);
        serverIconImage = file.get("server-icon.image", serverIconImage);

        randomMotd = file.get("random-motd", false);
    }
}
