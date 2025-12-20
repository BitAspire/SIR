package me.croabeast.sir.module.channel;

import lombok.Getter;
import lombok.Setter;
import me.croabeast.sir.ChatChannel;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@Getter @Setter
public class ChatEvent extends Event {

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    private final SIRUser user;
    private final Player player;

    private ChatChannel channel;
    private String message;

    private boolean global = false;

    public ChatEvent(SIRUser user, ChatChannel channel, String message, boolean async) {
        super(async);

        this.user = user;
        this.player = user.getPlayer();

        this.channel = channel;
        this.message = message;
    }

    public Set<SIRUser> getRecipients() {
        return channel.getRecipients(user);
    }

    void call() {
        Bukkit.getPluginManager().callEvent(this);
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlerList;
    }
}
