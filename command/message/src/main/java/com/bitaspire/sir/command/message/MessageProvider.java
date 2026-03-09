package com.bitaspire.sir.command.message;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.file.ConfigurableFile;
import com.bitaspire.sir.ChatToggleable;
import com.bitaspire.sir.ExtensionFile;
import com.bitaspire.sir.Listener;
import com.bitaspire.sir.command.SIRCommand;
import com.bitaspire.sir.command.StandaloneProvider;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public final class MessageProvider extends StandaloneProvider implements ChatToggleable {

    @Getter
    private final Set<SIRCommand> commands = new HashSet<>();
    Listener listener;

    final Map<CommandSender, CommandSender> replies = new HashMap<>();
    @Getter
    ConfigurableFile lang;

    @SneakyThrows
    public boolean register() {
        lang = new ExtensionFile(this, "lang", true);

        commands.add(new Message(this));
        commands.add(new Reply(this));

        return (listener = new Listener() {
            @EventHandler
            private void onQuit(PlayerQuitEvent event) {
                Player player = event.getPlayer();
                replies.entrySet().removeIf(e -> Objects.equals(e.getKey(), player) || Objects.equals(e.getValue(), player));
            }
        }).register();
    }

    @Override
    public boolean unregister() {
        return listener.unregister();
    }
}
