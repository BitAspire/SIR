package com.bitaspire.sir.module;

import com.bitaspire.sir.UserFormatter;
import me.croabeast.common.gui.ChestBuilder;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Module manager for SIR+.
 * Handles loading, unloading, and GUI configuration of modules.
 */
public interface ModuleManager {

    /**
     * Returns the list of loaded modules.
     * The list reflects modules that are currently known to the manager.
     *
     * @return list of modules.
     */
    @NotNull List<SIRModule> getModules();

    /**
     * Returns all known module names.
     *
     * @return module names.
     */
    @NotNull Set<String> getModuleNames();

    /**
     * Finds a module by name.
     * Implementations may normalize names for lookup.
     *
     * @param name module name.
     * @return module or {@code null} if not found.
     */
    SIRModule getModule(@NotNull String name);

    /**
     * Returns whether a module is enabled.
     * Disabled modules are not registered with the server.
     *
     * @param name module name.
     * @return {@code true} if enabled.
     */
    boolean isEnabled(String name);

    /**
     * Sets the enabled state of a module.
     * Implementations may persist this state for reloads.
     *
     * @param name module name.
     * @param enabled new state.
     */
    void setEnabled(String name, boolean enabled);

    /**
     * Updates the enabled state of a loaded module.
     * This may trigger registration or unregistration.
     *
     * @param module target module.
     * @param enabled new state.
     */
    void updateEnabled(SIRModule module, boolean enabled);

    /**
     * Updates the enabled state of a module by name.
     * Convenience wrapper around {@link #updateEnabled(SIRModule, boolean)}.
     *
     * @param name module name.
     * @param enabled new state.
     */
    void updateEnabled(String name, boolean enabled);

    /**
     * Opens the module configuration menu.
     * The menu lets users toggle module options.
     *
     * @param event click event that triggers the menu.
     */
    void openConfigMenu(@NotNull InventoryClickEvent event);

    /**
     * Returns a registered formatter by name.
     * Formatters are typically provided by modules.
     *
     * @param name formatter name.
     * @param <T> formatter reference type.
     * @return formatter or {@code null} if not found.
     */
    <T> UserFormatter<T> getFormatter(@NotNull String name);

    /**
     * Returns the join/quit service if present.
     * When absent, default join/quit behavior may apply.
     *
     * @return join/quit service or {@code null}.
     */
    JoinQuitService getJoinQuitService();

    /**
     * Returns the Discord service if present.
     * When absent, Discord integration is disabled.
     *
     * @return Discord service or {@code null}.
     */
    DiscordService getDiscordService();

    /**
     * Loads a module from an external jar.
     * Implementations may perform I/O and classloading.
     *
     * @param jarFile module jar file.
     * @param syncCommands {@code true} to synchronize commands.
     * @return {@code true} if loaded successfully.
     */
    boolean load(File jarFile, boolean syncCommands);

    /**
     * Loads all available modules.
     * Implementations typically scan the modules folder for jars.
     */
    void loadAll();

    /**
     * Retries loading deferred modules after a dependency becomes available.
     * This is usually triggered by a plugin enable event.
     *
     * @param pluginName plugin that just got enabled.
     * @param syncCommands {@code true} to synchronize commands.
     */
    void retryDeferredModules(String pluginName, boolean syncCommands);

    /**
     * Unloads a module by name.
     * Implementations should unregister listeners and release resources.
     *
     * @param name module name.
     */
    void unload(String name);

    /**
     * Unloads all modules.
     * This is typically used during shutdown or reload.
     */
    void unloadAll();

    /**
     * Returns the modules GUI menu builder.
     * The menu is used to toggle modules on or off.
     *
     * @return menu builder.
     */
    @NotNull ChestBuilder getMenu();

    /**
     * Persists enabled state for modules.
     * Implementations typically write to a states file.
     */
    void saveStates();
}
