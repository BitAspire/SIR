package com.bitaspire.sir.module.join;

import lombok.Getter;
import lombok.SneakyThrows;
import com.bitaspire.sir.file.ExtensionFile;

@Getter
final class Config {

    private final boolean joinDisabled, quitDisabled;
    private int joinCooldown = 0, betweenCooldown = 0, quitCooldown = 0;

    private final boolean spawnBeforeLogin;

    @SneakyThrows
    Config(JoinQuit main) {
        ExtensionFile file = new ExtensionFile(main, "config", true);

        joinDisabled = file.get("disable-vanilla-messages.join", false);
        quitDisabled = file.get("disable-vanilla-messages.quit", false);

        joinCooldown = file.getConfiguration().getInt("cooldown.join", joinCooldown);
        betweenCooldown = file.getConfiguration().getInt("cooldown.between", betweenCooldown);
        quitCooldown = file.getConfiguration().getInt("cooldown.quit", quitCooldown);

        spawnBeforeLogin = file.get("spawn-before-login", true);
    }
}
