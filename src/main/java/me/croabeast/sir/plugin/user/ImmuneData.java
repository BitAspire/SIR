package me.croabeast.sir.plugin.user;

/**
 * Represents a data structure for managing immunity status of a user in the SIR plugin.
 *
 * <p> This interface provides methods to check if a user is immune and to grant immunity for
 * a specified duration.
 * It is typically used to manage immunity against certain effects or actions within the game.
 *
 * <p> Implementations of this interface should handle the logic for tracking and applying
 * immunity status,including the duration of immunity and any related effects on the user.
 */
public interface ImmuneData {

    /**
     * Checks if the user is currently immune.
     * @return true if the user is immune, false otherwise
     */
    boolean isImmune();

    /**
     * Grants immunity to the user for a specified duration.
     * <p> The duration is specified in seconds. After this time, the user's immunity will expire.
     *
     * @param seconds the duration of immunity in seconds
     */
    void giveImmunity(int seconds);
}
