package me.croabeast.sir.module.moderation;

import lombok.SneakyThrows;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

final class Format extends Listener {

    final Moderation main;
    final ConfigurableFile file;

    @SneakyThrows
    Format(Moderation main) {
        this.main = main;
        this.file = new ExtensionFile(main, "format", true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onChatEvent(AsyncPlayerChatEvent event) {
        if (!file.get("enabled", true) ||
                main.getApi().getUserManager().hasPermission(
                        event.getPlayer(),
                        file.get("bypass", "sir.moderation.bypass.format")
                ))
            return;

        String message = event.getMessage();

        if (file.get("capitalize", false) && !message.isEmpty())
            message = Character.toUpperCase(message.charAt(0)) + message.substring(1);

        String charPath = "characters.";

        String prefix = file.get(charPath + "prefix", "");
        String suffix = file.get(charPath + "suffix", "");

        event.setMessage(prefix + message + suffix);
    }
}
