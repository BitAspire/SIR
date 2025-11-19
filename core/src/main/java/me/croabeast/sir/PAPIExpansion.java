package me.croabeast.sir;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for PlaceholderAPI expansions in the SIR plugin.
 * <p>
 * This class provides a structure for creating PlaceholderAPI expansions
 * with a defined identifier, author, and version. It requires subclasses to
 * implement the `onRequest` method to handle specific placeholder requests.
 */
@RequiredArgsConstructor
@Getter
public abstract class PAPIExpansion extends PlaceholderExpansion {

    /**
     * The identifier for this expansion, used by PlaceholderAPI.
     */
    private final String identifier;

    /**
     * Returns the required plugin for this expansion, which is always "SIR".
     * <p> This method is used by PlaceholderAPI to check if the required plugin is installed.
     *
     * @implNote This method is annotated with `@NotNull` to indicate that it will never return null.
     * @return The name of the required plugin, which is "SIR".
     */
    @NotNull
    public String getRequiredPlugin() {
        return "SIR";
    }

    /**
     * Returns the author of this expansion, always "CroaBeast".
     *
     * @return The author's name.
     */
    @NotNull
    public final String getAuthor() {
        return "CroaBeast";
    }

    /**
     * Returns the version of this expansion, always "1.0".
     *
     * @return The version of the expansion.
     */
    @NotNull
    public final String getVersion() {
        return "1.0";
    }

    /**
     * Handles requests for placeholders defined in this expansion.
     * <p>
     * Subclasses must implement this method to provide the logic for
     * handling specific placeholder requests.
     *
     * @param off    The offline player requesting the placeholder.
     * @param params The parameters for the placeholder request.
     * @return The value of the requested placeholder, or null if not applicable.
     */
    @Nullable
    public abstract String onRequest(OfflinePlayer off, @NotNull String params);
}
