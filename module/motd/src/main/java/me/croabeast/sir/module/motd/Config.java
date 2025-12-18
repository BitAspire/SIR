package me.croabeast.sir.module.motd;

import lombok.Getter;
import me.croabeast.sir.ExtensionFile;

@Getter
final class Config {

    private boolean alwaysLoadDefaultIcon = true;

    private String maxPlayersType = "DEFAULT";
    private int maxPlayersCount = 69420;

    private String serverIconUsage = "SINGLE";
    private String serverIconImage = "server-icon.png";

    private boolean randomMotd = false;

    Config(MOTD main) {
        try {
            ExtensionFile file = new ExtensionFile(main, "config", true);

            alwaysLoadDefaultIcon = file.get("always-load-default-icon", true);

            maxPlayersType = file.get("max-players.type", maxPlayersType);
            maxPlayersCount = file.get("max-players.count", maxPlayersCount);

            serverIconUsage = file.get("server-icon.usage", serverIconUsage);
            serverIconImage = file.get("server-icon.image", serverIconImage);

            randomMotd = file.get("random-motd", false);
        } catch (Exception ignored) {}
    }
}
