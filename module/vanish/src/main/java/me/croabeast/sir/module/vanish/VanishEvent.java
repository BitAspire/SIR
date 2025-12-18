package me.croabeast.sir.module.vanish;

import lombok.Getter;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class VanishEvent extends Event {

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    private final SIRUser user;
    private final boolean vanished;

    public VanishEvent(SIRUser user, boolean vanished) {
        this.user = user;
        this.vanished = vanished;
    }

    public void call() {
        Bukkit.getPluginManager().callEvent(this);
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlerList;
    }
}
