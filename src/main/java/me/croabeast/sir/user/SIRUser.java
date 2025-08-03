package me.croabeast.sir.user;

import me.croabeast.sir.manager.UserManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

/**
 * Represents a user in the SIR plugin.
 *
 * <p> This interface provides methods to access user data and perform actions related to the user.
 * It includes methods for retrieving user information, checking permissions, playing sounds,
 * and managing user states such as online status, logged status, and vanish state.
 */
public interface SIRUser {

    /**
     * Gets the offline player representation of this user.
     * @return the offline player
     */
    @NotNull
    OfflinePlayer getOffline();

    /**
     * Gets the online player representation of this user.
     * @return the online player
     */
    Player getPlayer();

    /**
     * Gets the name of this user.
     * @return the name of the user
     */
    @NotNull
    String getName();

    /**
     * Gets the unique identifier (UUID) of this user.
     * @return the UUID of the user
     */
    @NotNull
    UUID getUuid();

    /**
     * Gets the prefix of this user. The prefix is typically used for chat formatting.
     * @return the prefix of the user, or null if not set
     */
    @Nullable
    String getPrefix();

    /**
     * Gets the suffix of this user. The suffix is typically used for chat formatting.
     * @return the suffix of the user, or null if not set
     */
    @Nullable
    String getSuffix();

    /**
     * Checks if this user has a specific permission.
     * @param permission the permission to check
     * @return true if the user has the permission, false otherwise
     */
    default boolean hasPermission(String permission) {
        return UserManager.hasPermission(this, permission);
    }

    /**
     * Checks if this user is online. A user is considered online if they have an associated online player instance.
     * @return true if the user is online, false otherwise
     */
    boolean isOnline();

    /**
     * Checks if this user is logged in. A user is considered logged in if they have completed the login process.
     * @return true if the user is logged in, false otherwise
     */
    boolean isLogged();

    /**
     * Sets the logged status of this user.
     * @param logged true to mark the user as logged in, false otherwise
     */
    void setLogged(boolean logged);

    /**
     * Checks if this user is vanished. A vanished user is typically hidden from other players.
     * @return true if the user is vanished, false otherwise
     */
    boolean isVanished();

    /**
     * Gets the data associated with this user for ignoring other users.
     *
     * <p> This data typically includes information about which users this user is ignoring.
     *
     * @return the ignore data for this user
     * @see IgnoreData
     */
    @NotNull
    IgnoreData getIgnoreData();

    /**
     * Gets the data associated with this user for being muted, which includes information about
     * how long the user is muted and the reason for the mute.
     *
     * @return the mute data for this user
     * @see MuteData
     */
    @NotNull
    MuteData getMuteData();

    /**
     * Gets the data associated with this user for any local channel they are in.
     *
     * <p> This data typically includes information about the channels the user is subscribed to,
     * their preferences for those channels, and any other relevant channel-related settings.
     *
     * @return the channel data for this user
     * @see ChannelData
     */
    @NotNull
    ChannelData getChannelData();

    /**
     * Gets the data associated with this user for color settings.
     * <p> This data typically includes information about the user's preferred colors for chat and other elements.
     *
     * @return the color data for this user
     * @see ColorData
     */
    @NotNull
    ColorData getColorData();

    /**
     * Gets the data associated with this user for immune status.
     *
     * <p> This data typically includes information about the user's immunity status, such as whether
     * they are immune to certain effects or actions within the game.
     *
     * @return the immune data for this user
     * @see ImmuneData
     */
    @NotNull
    ImmuneData getImmuneData();

    /**
     * Plays a sound for this user at the player's current location.
     *
     * @param sound the sound to play
     * @param volume the volume of the sound
     * @param pitch the pitch of the sound
     */
    default void playSound(Sound sound, float volume, float pitch) {
        final Player p = getPlayer();
        p.playSound(p.getLocation(), sound, volume, pitch);
    }

    /**
     * Plays a sound for this user at the player's current location with default volume and pitch.
     * @param sound the sound to play
     */
    default void playSound(Sound sound) {
        playSound(sound, 1.0f, 1.0f);
    }

    /**
     * Plays a sound for this user using a raw sound string.
     * <p> The raw sound string is converted to a Sound enum value. If the sound is not valid, the method does nothing.
     *
     * @param rawSound the raw sound string to play
     * @param volume the volume of the sound
     * @param pitch the pitch of the sound
     */
    default void playSound(String rawSound, float volume, float pitch) {
        Sound sound;
        try {
            sound = Sound.valueOf(rawSound);
        } catch (Exception e) {
            return;
        }

        playSound(sound, volume, pitch);
    }

    /**
     * Plays a sound for this user using a raw sound string with default volume and pitch.
     * <p> The raw sound string is converted to a Sound enum value. If the sound is not valid, the method does nothing.
     *
     * @param sound the raw sound string to play
     */
    default void playSound(String sound) {
        playSound(sound, 1.0f, 1.0f);
    }

    /**
     * Gets a set of nearby users within a specified range.
     * <p> This method retrieves all users that are within the specified range of this user.
     *
     * @param range the range within which to find nearby users, in blocks
     * @return a set of nearby users within the specified range
     */
    @NotNull
    Set<SIRUser> getNearbyUsers(double range);
}
