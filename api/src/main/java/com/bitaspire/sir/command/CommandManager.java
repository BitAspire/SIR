package com.bitaspire.sir.command;

import com.bitaspire.sir.addon.SIRAddon;
import com.bitaspire.sir.module.SIRModule;
import me.croabeast.command.Synchronizer;
import me.croabeast.common.gui.ChestBuilder;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.Set;

/**
 * Command and provider manager for SIR+.
 * Coordinates provider discovery, registration, and state persistence.
 */
public interface CommandManager {

    /**
     * Loads embedded commands from a module.
     * The module must include a {@code commands.yml} on its classpath.
     *
     * @param module source module.
     * @param syncCommands {@code true} to synchronize commands.
     */
    void loadFromModule(@NotNull SIRModule module, boolean syncCommands);

    /**
     * Loads embedded commands from an addon.
     * The addon must include a {@code commands.yml} on its classpath.
     *
     * @param addon source addon.
     * @param syncCommands {@code true} to synchronize commands.
     */
    default void loadFromAddon(@NotNull SIRAddon addon, boolean syncCommands) {
        throw new UnsupportedOperationException("Addon command providers are only available in SIR+");
    }

    /**
     * Loads a provider from an external jar.
     * Implementations may perform classloading and dependency checks.
     *
     * @param jarFile jar file.
     * @param syncCommands {@code true} to synchronize commands.
     */
    void load(File jarFile, boolean syncCommands);

    /**
     * Loads all available providers.
     * Implementations typically scan the commands folder for jars.
     */
    void loadAll();

    /**
     * Retries loading deferred providers after a dependency becomes available.
     * This is usually triggered by a plugin enable event.
     *
     * @param pluginName plugin that just got enabled.
     * @param syncCommands {@code true} to synchronize commands.
     */
    void retryDeferredProviders(String pluginName, boolean syncCommands);

    /**
     * Returns all loaded commands.
     * The returned set reflects currently registered commands.
     *
     * @return set of commands.
     */
    @NotNull Set<SIRCommand> getCommands();

    /**
     * Finds a command by name.
     * Implementations may normalize names for lookup.
     *
     * @param name command name.
     * @return command or {@code null} if not found.
     */
    SIRCommand getCommand(String name);

    /**
     * Returns whether a command is enabled.
     * Disabled commands are not registered with Bukkit.
     *
     * @param name command name.
     * @return {@code true} if enabled.
     */
    boolean isEnabled(String name);

    /**
     * Returns the command settings service if present.
     * This service controls per-user toggles.
     *
     * @return settings service or {@code null}.
     */
    SettingsService getSettingsService();

    /**
     * Returns the command synchronizer used by the manager.
     * <p>
     * This synchronizer is responsible for syncing registrations with Bukkit's
     * command map after changes such as enabling, disabling, or overriding
     * commands. Implementations may debounce or schedule syncs to avoid excessive
     * command map rebuilds, so calls to {@link Synchronizer#sync()} may not apply
     * immediately.
     * <p>
     * Use this when you manage commands manually and need to ensure Bukkit sees
     * the updated registrations.
     *
     * @return command synchronizer.
     */
    @NotNull Synchronizer getSynchronizer();

    /**
     * Returns the loaded providers.
     * The collection reflects providers currently known by the manager.
     *
     * @return collection of providers.
     */
    @NotNull Collection<CommandProvider> getProviders();

    /**
     * Returns the display names of all known standalone providers.
     *
     * @return provider names.
     */
    @NotNull Set<String> getProviderNames();

    /**
     * Returns command keys declared by a provider.
     *
     * @param providerName provider name.
     * @return command keys.
     */
    @NotNull Set<String> getProviderCommands(String providerName);

    /**
     * Returns provider information by name.
     * This information is parsed from {@code commands.yml}.
     *
     * @param name provider name.
     * @return provider information or {@code null}.
     */
    ProviderInformation getInformation(String name);

    /**
     * Returns whether a provider is enabled.
     * Disabled providers are skipped during registration.
     *
     * @param main provider main name.
     * @return {@code true} if enabled.
     */
    boolean isProviderEnabled(String main);

    /**
     * Sets the enabled state of a provider.
     * Implementations may persist this state for reloads.
     *
     * @param main provider main name.
     * @param enabled new state.
     */
    void setProviderEnabled(String main, boolean enabled);

    /**
     * Updates the enabled state of a provider.
     * This may trigger registration or unregistration of commands.
     *
     * @param providerName provider name.
     * @param enabled new state.
     * @param syncCommands {@code true} to synchronize commands.
     * @return {@code true} if the change was applied.
     */
    boolean updateProviderEnabled(String providerName, boolean enabled, boolean syncCommands);

    /**
     * Returns whether a command is forced to override.
     * A {@code null} value indicates no explicit override state.
     *
     * @param providerName provider name.
     * @param commandKey command key.
     * @return {@code true}, {@code false}, or {@code null} if no state exists.
     */
    Boolean getCommandOverride(String providerName, String commandKey);

    /**
     * Updates a command override flag.
     * When updated, the command may be re-registered.
     *
     * @param providerName provider name.
     * @param commandKey command key.
     * @param override new override state.
     * @param syncCommands {@code true} to synchronize commands.
     * @return {@code true} if the change was applied.
     */
    boolean updateCommandOverride(String providerName, String commandKey, boolean override, boolean syncCommands);

    /**
     * Returns the commands GUI menu builder.
     * The menu is used to toggle providers and commands.
     *
     * @return menu builder.
     */
    @NotNull ChestBuilder getMenu();

    /**
     * Opens the override menu for a standalone provider.
     * The menu allows enabling or disabling overrides per command.
     *
     * @param provider target provider.
     * @param event click event.
     * @param syncCommands {@code true} to synchronize commands.
     */
    void openOverrideMenu(@NotNull StandaloneProvider provider, @NotNull InventoryClickEvent event, boolean syncCommands);

    /**
     * Unloads a provider by name.
     * Implementations should unregister commands and release resources.
     *
     * @param name provider name.
     * @param syncCommands {@code true} to synchronize commands.
     */
    void unload(String name, boolean syncCommands);

    /**
     * Unloads a specific provider.
     * Implementations should unregister commands and release resources.
     *
     * @param provider provider to unload.
     * @param syncCommands {@code true} to synchronize commands.
     */
    void unload(CommandProvider provider, boolean syncCommands);

    /**
     * Unloads all providers.
     * This is typically used during shutdown or reload.
     */
    void unloadAll();

    /**
     * Persists provider states and overrides.
     * Implementations typically write to a states file.
     */
    void saveStates();
}
