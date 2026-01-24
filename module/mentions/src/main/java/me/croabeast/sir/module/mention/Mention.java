package me.croabeast.sir.module.mention;

import lombok.Getter;
import me.croabeast.file.Configurable;
import me.croabeast.sir.PermissibleUnit;
import me.croabeast.sir.SoundSection;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

@Getter
final class Mention implements PermissibleUnit {

    private final ConfigurationSection section;

    private final String prefix, click, value;
    private final List<String> hover;

    private final List<String> senderMessages, receiverMessages;
    private SoundSection senderSound, receiverSound;

    Mention(ConfigurationSection section) {
        this.section = section;

        prefix = section.getString("prefix");
        click = section.getString("click");
        value = section.getString("value");

        hover = Configurable.toStringList(section, "hover");
        senderMessages = Configurable.toStringList(section, "messages.sender");
        receiverMessages = Configurable.toStringList(section, "messages.receiver");

        try {
            SoundSection temp = new SoundSection(section.getConfigurationSection("sound.sender"));
            if (temp.isEnabled()) senderSound = temp;
        } catch (Exception ignored) {}

        try {
            SoundSection temp = new SoundSection(section.getConfigurationSection("sound.receiver"));
            if (temp.isEnabled()) receiverSound = temp;
        } catch (Exception ignored) {}
    }
}
