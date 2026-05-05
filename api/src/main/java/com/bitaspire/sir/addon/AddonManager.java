package com.bitaspire.sir.addon;

import com.bitaspire.sir.UserFormatter;
import me.croabeast.common.gui.ChestBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Addon manager for SIR+.
 * Handles loading, unloading, and GUI presentation of addons.
 */
public interface AddonManager {

    /**
     * Returns the list of loaded addons.
     * The list reflects currently loaded and known addon instances.
     *
     * @return list of addons.
     */
    @NotNull List<SIRAddon> getAddons();

    /**
     * Returns all known addon names.
     *
     * @return addon names.
     */
    @NotNull Set<String> getAddonNames();

    /**
     * Finds an addon by name.
     * The lookup is typically case-insensitive depending on implementation.
     *
     * @param name addon name.
     * @return addon or {@code null} if not found.
     */
    SIRAddon getAddon(@NotNull String name);

    /**
     * Returns whether an addon is enabled.
     * Disabled addons are not registered with the server.
     *
     * @param name addon name.
     * @return {@code true} if enabled.
     */
    boolean isEnabled(String name);

    /**
     * Sets the enabled state of an addon.
     * Implementations may persist this state for future reloads.
     *
     * @param name addon name.
     * @param enabled new state.
     */
    void setEnabled(String name, boolean enabled);

    /**
     * Updates the enabled state of a loaded addon.
     * This may trigger registration or unregistration.
     *
     * @param addon target addon.
     * @param enabled new state.
     */
    void updateEnabled(SIRAddon addon, boolean enabled);

    /**
     * Updates the enabled state of an addon by name.
     * Convenience wrapper around {@link #updateEnabled(SIRAddon, boolean)}.
     *
     * @param name addon name.
     * @param enabled new state.
     */
    void updateEnabled(String name, boolean enabled);

    /**
     * Returns a registered formatter by name.
     * Formatters are typically provided by addons.
     *
     * @param name formatter name.
     * @param <T> formatter reference type.
     * @return formatter or {@code null} if not found.
     */
    <T> UserFormatter<T> getFormatter(@NotNull String name);

    /**
     * Loads an addon from an external jar.
     * Implementations may perform I/O and classloading.
     *
     * @param jarFile addon jar file.
     * @param syncCommands {@code true} to synchronize commands.
     * @return {@code true} if loaded successfully.
     */
    boolean load(File jarFile, boolean syncCommands);

    /**
     * Loads all available addons.
     * Implementations typically scan the addons folder for jars.
     */
    void loadAll();

    /**
     * Retries loading deferred addons after a dependency becomes available.
     * This is usually called when a plugin dependency is enabled.
     *
     * @param pluginName plugin that just got enabled.
     * @param syncCommands {@code true} to synchronize commands.
     */
    void retryDeferredAddons(String pluginName, boolean syncCommands);

    /**
     * Unloads an addon by name.
     * Implementations should release resources and unregister listeners.
     *
     * @param name addon name.
     */
    void unload(String name);

    /**
     * Unloads all addons.
     * This is typically used during shutdown or reload.
     */
    void unloadAll();

    /**
     * Returns the addons GUI menu builder.
     * The menu is used to toggle addons on or off.
     *
     * @return menu builder.
     */
    @NotNull ChestBuilder getMenu();

    /**
     * Persists enabled state for addons.
     * Implementations usually store this under a states file.
     */
    void saveStates();
}
