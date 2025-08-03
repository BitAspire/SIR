package me.croabeast.sir.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Represents color data for a user in the SIR plugin.
 *
 * <p> This interface provides methods to manage color formatting for user messages.
 * It includes methods for setting and retrieving color start and end formats, as
 * well as managing the formats available to the user.
 * The color formats are typically used for chat messages, titles, and other text
 * elements.
 *
 * <p> Implementations of this interface should ensure that the color formats are
 * compatible with the chat system used by the SIR plugin.
 * The start format is applied at the beginning of a message, while the end format
 * is applied at the end of a message, allowing for flexible text styling.
 */
public interface ColorData {

    /**
     * Gets the set of color formats available to this user.
     * @return a set of color format strings
     */
    @NotNull
    Set<String> getFormats();

    /**
     * Sets the color start format for this user.
     * @param start the color start format string, must not be null
     */
    void setColorStart(@NotNull String start);

    /**
     * Sets the color end format for this user.
     * @param end the color end format string, can be null
     */
    void setColorEnd(@Nullable String end);

    /**
     * Gets the color start format for this user.
     * @return the color start format string, must not be null
     */
    @NotNull
    String getStart();

    /**
     * Gets the color end format for this user.
     * @return the color end format string, can be null
     */
    @Nullable
    String getEnd();

    /**
     * Removes all color formats from this user.
     *
     * <p> This method clears the set of color formats, effectively removing any
     * custom color formatting that was previously set for the user.
     * It is typically used to reset the user's color formatting to default.
     */
    void removeAnyFormats();
}
