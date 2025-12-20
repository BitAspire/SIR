package me.croabeast.sir.module.mention;

import lombok.Getter;
import me.croabeast.file.Configurable;
import me.croabeast.sir.PermissibleUnit;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

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
            if (temp.enabled) senderSound = temp;
        } catch (Exception ignored) {}

        try {
            SoundSection temp = new SoundSection(section.getConfigurationSection("sound.receiver"));
            if (temp.enabled) receiverSound = temp;
        } catch (Exception ignored) {}
    }

    static <T> T non(T value) {
        return Objects.requireNonNull(value);
    }

    static class SoundSection {

        private final boolean enabled;
        private final Sound sound;
        private float volume = 1.0F, pitch = 1.0F;

        SoundSection(@Nullable ConfigurationSection section) {
            enabled = non(section).getBoolean("enabled");
            sound = Sound.valueOf(section.getString("type"));

            try {
                String temp = non(section.getString("volume"));
                volume = Float.parseFloat(temp);
            } catch (Exception ignored) {}

            try {
                String temp = non(section.getString("pitch"));
                pitch = Float.parseFloat(temp);
            } catch (Exception ignored) {}
        }

        void playSound(SIRUser user) {
            user.playSound(sound, volume, pitch);
        }
    }
}
