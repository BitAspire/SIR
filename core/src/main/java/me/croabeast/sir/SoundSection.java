package me.croabeast.sir;

import lombok.Getter;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a sound configuration section that can be used to play sounds to users.
 * <p> This class parses sound settings from a configuration section and provides
 * methods to play the configured sound to a user.
 */
@Getter
public final class SoundSection {

    private final boolean enabled;
    private final Sound sound;
    private float volume = 1.0F, pitch = 1.0F;

    /**
     * Creates a new SoundSection from a configuration section.
     *
     * @param section the configuration section containing sound settings
     */
    public SoundSection(@Nullable ConfigurationSection section) {
        boolean enabledTemp = section != null && section.getBoolean("enabled");
        sound = SoundUtils.parseSound(section != null ? section.getString("type") : null);
        enabled = enabledTemp && sound != null;

        if (section != null) {
            try {
                String temp = section.getString("volume");
                if (temp != null) volume = Float.parseFloat(temp);
            } catch (NumberFormatException ignored) {}

            try {
                String temp = section.getString("pitch");
                if (temp != null) pitch = Float.parseFloat(temp);
            } catch (NumberFormatException ignored) {}
        }
    }

    /**
     * Plays the configured sound to the specified user.
     * <p> Does nothing if the sound is not enabled or the user is not online.
     *
     * @param user the user to play the sound to
     */
    public void playSound(SIRUser user) {
        if (enabled && sound != null) user.playSound(sound, volume, pitch);
    }
}
