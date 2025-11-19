package me.croabeast.sir.user;

/**
 * Interface representing channel data for a user in the SIR plugin.
 *
 * <p> This interface provides methods to toggle channel visibility and check
 * if a channel is toggled on for the user.
 * It is used to manage user preferences regarding which channels they want
 * to see or hide in the plugin.
 *
 * <p> Implementations of this interface should handle the logic for toggling
 * channels and storing the state of each channel for the user.
 */
public interface ChannelData {

    /**
     * Toggles the specified channel visibility for the user.
     * @param channel the name of the channel to toggle
     */
    void toggle(String channel);

    /**
     * Checks if the specified channel visibility is toggled on for the user.
     * @param channel the name of the channel to check
     * @return true if the channel is toggled on, false otherwise
     */
    boolean isToggled(String channel);
}
