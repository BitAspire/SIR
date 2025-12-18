package me.croabeast.sir.module.join;

import lombok.Getter;
import me.croabeast.sir.ExtensionFile;

@Getter
final class Config {

    private boolean joinDisabled = false, quitDisabled = false;
    private int joinCooldown = 0, betweenCooldown = 0, quitCooldown = 0;

    private boolean spawnBeforeLogin = true;

    Config(JoinQuit main) {
        try {
            ExtensionFile file = new ExtensionFile(main, "config", true);

            joinDisabled = file.get("disable-vanilla-messages.join", false);
            quitDisabled = file.get("disable-vanilla-messages.quit", false);

            joinCooldown = file.get("cooldown.join", joinCooldown);
            betweenCooldown = file.get("cooldown.between", betweenCooldown);
            quitCooldown = file.get("cooldown.quit", quitCooldown);

            spawnBeforeLogin = file.get("spawn-before-login", true);
        } catch (Exception ignored) {}
    }
}
