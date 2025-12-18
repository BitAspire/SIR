package me.croabeast.sir.module.cooldown;

import lombok.Getter;
import me.croabeast.file.Configurable;
import me.croabeast.sir.PermissibleUnit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

@Getter
final class CooldownUnit implements PermissibleUnit {

    private final ConfigurationSection section;

    private final boolean spamEnabled;
    private final int spamCount, spamTimeLimit;
    private final List<String> spamCommands;

    private final int time;
    private final List<String> messages;

    CooldownUnit(ConfigurationSection section) {
        this.section = section;

        spamEnabled = section.getBoolean("spam.enabled");
        spamCount = section.getInt("spam.count");
        spamTimeLimit = section.getInt("spam.time-limit");
        spamCommands = Configurable.toStringList(section, "spam.commands");

        time = section.getInt("time");
        messages = Configurable.toStringList(section, "messages");
    }
}
