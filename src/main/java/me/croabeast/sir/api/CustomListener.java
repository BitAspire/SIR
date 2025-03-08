package me.croabeast.sir.api;

import me.croabeast.lib.Registrable;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * A custom interface that extends the {@link Listener} interface from the Bukkit API.
 * <p>
 * This interface provides methods to register and unregister listeners for events.
 * It includes methods to check the registration status and manage the registration state.
 */
public interface CustomListener extends Listener, Registrable {

    /**
     * Checks if this listener is currently registered.
     * @return {@code true} if the listener is registered to handle events, {@code false} otherwise.
     */
    boolean isRegistered();

    /**
     * Sets the registration status of this listener.
     * <p>
     * This method is used to update the internal state of the listener regarding its registration status.
     *
     * @param registered {@code true} to mark the listener as registered, {@code false} to mark it as unregistered.
     */
    void setRegistered(boolean registered);

    /**
     * Registers this listener for the given plugin.
     * <p>
     * This method registers the listener to the plugin, so it can handle events. It will only register
     * the listener if it is not already registered.
     *
     * @param plugin The plugin to register this listener for. Must not be null.
     *
     * @return {@code true} if the listener was successfully registered, {@code false} if it was already registered.
     * @throws NullPointerException if the plugin is {@code null}.
     */
    default boolean register(Plugin plugin) {
        if (isRegistered()) return false;

        Bukkit.getPluginManager().registerEvents(this, Objects.requireNonNull(plugin));
        setRegistered(true);

        return true;
    }

    /**
     * Registers this listener using the plugin that provides this listener class.
     * <p>
     * This method uses {@link JavaPlugin#getProvidingPlugin} to determine the plugin that provides this listener
     * and registers the listener to that plugin, so it can handle events. It will only register the listener
     * if it is not already registered.
     *
     * @return {@code true} if the listener was successfully registered, {@code false} if it was already registered.
     */
    default boolean register() {
        return register(JavaPlugin.getProvidingPlugin(CustomListener.class));
    }

    /**
     * Unregisters this listener from all registered events.
     * <p>
     * This method unregisters the listener from the plugin manager, stopping it from handling any further events.
     * It will only unregister the listener if it is currently registered.
     *
     * @return {@code true} if the listener was successfully unregistered, {@code false} if it was not registered.
     */
    default boolean unregister() {
        if (!isRegistered()) return false;

        HandlerList.unregisterAll(this);
        setRegistered(false);
        return true;
    }
}
