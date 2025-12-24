package me.croabeast.sir;

import lombok.Getter;
import me.croabeast.common.Registrable;
import me.croabeast.takion.logger.LogLevel;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

@Getter
public abstract class Listener implements Registrable, org.bukkit.event.Listener {

    private boolean registered = false;

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
