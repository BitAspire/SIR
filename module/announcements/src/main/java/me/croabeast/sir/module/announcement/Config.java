package me.croabeast.sir.module.announcement;

import lombok.Getter;
import me.croabeast.sir.ExtensionFile;

@Getter
final class Config {

    private int interval = 2400;
    private boolean random = false;

    Config(Announcements main) {
        try {
            ExtensionFile file = new ExtensionFile(main, "config", true);
            interval = file.get("interval", interval);
            random = file.get("random", false);
        } catch (Exception ignored) {}
    }
}
