package me.croabeast.sir.user;

/**
 * Represents mute data for a user in the SIR plugin.
 *
 * <p> This interface provides methods to manage mute status, including muting, unmuting,
 * and retrieving mute details such as reason, who muted the user, and remaining mute time.
 *
 * <p> Implementations of this interface should handle the logic for muting and unmuting users,
 * as well as storing and retrieving the necessary data.
 */
public interface MuteData {

    /**
     * Checks if the user is currently muted.
     * @return true if the user is muted, false otherwise
     */
    boolean isMuted();

    /**
     * Mutes the user for a specified duration with a reason and the name of the person who muted them.
     *
     * @param time the duration of the mute in milliseconds
     * @param reason the reason for the mute
     * @param by the name of the person who muted the user
     */
    void mute(long time, String reason, String by);

    /**
     * Mutes the user for a specified duration.
     * @param time the duration of the mute in milliseconds
     */
    void mute(long time);

    /**
     * Unmutes the user, removing any mute status they have.
     */
    void unmute();

    /**
     * Gets the reason for the user's mute.
     * @return the reason for the mute, or null if not muted
     */
    String getReason();

    /**
     * Gets the name of the person who muted the user.
     * @return the name of the person who muted the user, or null if not muted
     */
    String getMuteBy();

    /**
     * Gets the time remaining for the user's mute.
     * @return the remaining mute time in milliseconds, or 0 if not muted
     */
    long getRemaining();
}
