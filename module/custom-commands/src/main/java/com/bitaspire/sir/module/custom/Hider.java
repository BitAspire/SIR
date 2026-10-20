package com.bitaspire.sir.module.custom;

import com.bitaspire.sir.Listener;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.Collection;
import java.util.Locale;

@RequiredArgsConstructor
final class Hider extends Listener {

    private final CustomCommands main;

    @EventHandler
    private void onSend(PlayerCommandSendEvent event) {
        String prefix = main.getApi().getPlugin().getName().toLowerCase(Locale.ENGLISH) + ':';
        Collection<String> labels = event.getCommands();

        for (CustomCommand command : main.loaded.values()) {
            if (!command.isHidden()) continue;

            labels.remove(command.getName());
            labels.remove(prefix + command.getName());

            for (String alias : command.getAliases()) {
                labels.remove(alias);
                labels.remove(prefix + alias);
            }
        }
    }
}
