package me.croabeast.sir;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for modules/commands that depend on external plugins.
 * <p>
 * Implement this interface to declare hard dependencies (required plugins)
 * and soft dependencies (optional plugins that enable extra features).
 */
public interface PluginDependant {

    /**
     * Returns the hard dependencies (required plugins).
     * The module/command will not load if any of these plugins are missing.
     *
     * @return array of required plugin names, or empty array if none
     */
    @NotNull
    default String[] getDependencies() {
        return new String[0];
    }

    /**
     * Returns the soft dependencies (optional plugins).
     * The module/command will still load without these, but some features may be disabled.
     *
     * @return array of optional plugin names, or empty array if none
     */
    @NotNull
    default String[] getSoftDependencies() {
        return new String[0];
    }

    /**
     * Checks if all hard dependencies are enabled.
     *
     * @return true if all required plugins are available and enabled
     */
    default boolean areDependenciesEnabled() {
        for (String dependency : getDependencies())
            if (!isPluginEnabled(dependency)) return false;

        return true;
    }

    /**
     * Alias for {@link #areDependenciesEnabled()}.
     * Checks if all hard dependencies are enabled.
     *
     * @return true if all required plugins are available and enabled
     * @deprecated Use {@link #areDependenciesEnabled()} instead
     */
    @Deprecated
    default boolean isPluginEnabled() {
        return areDependenciesEnabled();
    }

    /**
     * Checks if at least one hard dependency is enabled.
     * Useful when only one of multiple plugins is required.
     *
     * @return true if at least one required plugin is available
     */
    default boolean isAnyDependencyEnabled() {
        String[] deeps = getDependencies();
        if (deeps.length == 0) return true;

        for (String dependency : deeps)
            if (isPluginEnabled(dependency)) return true;

        return false;
    }

    /**
     * Gets a plugin instance by name if it's enabled.
     *
     * @param pluginName the name of the plugin
     * @return the plugin instance, or null if not available or not enabled
     */
    @Nullable
    default Plugin getPlugin(@Nullable String pluginName) {
        if (pluginName == null || pluginName.trim().isEmpty()) return null;

        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        return (plugin != null && plugin.isEnabled()) ? plugin : null;
    }

    /**
     * Checks if a specific plugin is enabled.
     *
     * @param pluginName the name of the plugin to check
     * @return true if the plugin is available and enabled
     */
    default boolean isPluginEnabled(@Nullable String pluginName) {
        return getPlugin(pluginName) != null;
    }
}
