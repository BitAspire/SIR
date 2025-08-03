package me.croabeast.sir;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an extension for the SIR plugin.
 *
 * <p> Implementations of this interface provide methods to retrieve information about the extension's name,
 * its loading status, and whether it is enabled.
 */
public interface SIRExtension {

    /**
     * Gets the name of the extension.
     * @return The name of the extension.
     */
    @NotNull String getName();

    /**
     * Checks if the extension is loaded.
     * @return True if the extension is loaded, false otherwise.
     */
    boolean isLoaded();

    /**
     * Checks if the extension is enabled.
     * @return True if the extension is enabled, false otherwise.
     */
    boolean isEnabled();
}
