package me.croabeast.sir.module.announcement;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.sir.ExtensionFile;

@Getter
final class Config {

    private int interval = 2400;
    private final boolean random;

    @SneakyThrows
    Config(Announcements main) {
        ExtensionFile file = new ExtensionFile(main, "config", true);
        interval = file.get("interval", interval);
        random = file.get("random", false);
    }
}
