package com.bitaspire.sir.module.announcement;

import lombok.Getter;
import lombok.SneakyThrows;
import com.bitaspire.sir.file.ExtensionFile;

@Getter
final class Config {

    private int interval = 2400;
    private final boolean random;

    @SneakyThrows
    Config(Announcements main) {
        ExtensionFile file = new ExtensionFile(main, "config", true);
        interval = file.getConfiguration().getInt("interval", interval);
        random = file.get("random", false);
    }
}
