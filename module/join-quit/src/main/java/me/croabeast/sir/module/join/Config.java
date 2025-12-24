package me.croabeast.sir.module.join;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.sir.ExtensionFile;

@Getter
final class Config {

    private final boolean joinDisabled, quitDisabled;
    private int joinCooldown = 0, betweenCooldown = 0, quitCooldown = 0;

    private boolean spawnBeforeLogin = true;

    @SneakyThrows
    Config(JoinQuit main) {
        ExtensionFile file = new ExtensionFile(main, "config", true);

        joinDisabled = file.get("disable-vanilla-messages.join", false);
        quitDisabled = file.get("disable-vanilla-messages.quit", false);

        joinCooldown = file.get("cooldown.join", joinCooldown);
        betweenCooldown = file.get("cooldown.between", betweenCooldown);
        quitCooldown = file.get("cooldown.quit", quitCooldown);

        spawnBeforeLogin = file.get("spawn-before-login", true);
    }
}
