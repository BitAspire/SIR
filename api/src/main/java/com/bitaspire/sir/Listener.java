package com.bitaspire.sir;

import lombok.Getter;
import me.croabeast.common.Registrable;
import me.croabeast.takion.logger.LogLevel;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

/**
 * Base class for SIR event listeners.
 *
 * <p> Wraps Bukkit event registration and unregistration with error logging
 * and tracks the registration state automatically.
 */
@Getter
public abstract class Listener implements Registrable, org.bukkit.event.Listener {

    /** Whether this listener is currently registered with Bukkit. */
    private boolean registered = false;

    /**
     * Registers this listener with the Bukkit plugin manager.
     *
     * <p> Does nothing if already registered.
     *
     * @return {@code true} if registration succeeded or was already active.
     */
    @Override
    public boolean register() {
        if (registered) return true;

        try {
            Bukkit.getPluginManager().registerEvents(this, SIRApi.instance().getPlugin());
            return registered = true;
        } catch (Exception e) {
            SIRApi.instance().getLibrary().getLogger().log(LogLevel.ERROR, "Failed to register listener: " + getClass().getName());
            e.printStackTrace();
            registered = false;
            return false;
        }
    }

    /**
     * Unregisters this listener from all Bukkit handler lists.
     *
     * <p> Does nothing if already unregistered.
     *
     * @return {@code true} if unregistration succeeded or was already inactive.
     */
    @Override
    public boolean unregister() {
        if (!registered) return true;

        try {
            HandlerList.unregisterAll(this);
            registered = false;
            return true;
        } catch (Exception e) {
            SIRApi.instance().getLibrary().getLogger().log(LogLevel.ERROR, "Failed to unregister listener: " + getClass().getName());
            e.printStackTrace();
            registered = true;
            return false;
        }
    }
}
