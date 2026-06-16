package com.bitaspire.sir.module.moderation;

import com.bitaspire.sir.chat.ChatProcessor;
import com.bitaspire.sir.user.SIRUser;
import lombok.SneakyThrows;
import me.croabeast.file.ConfigurableFile;
import com.bitaspire.sir.file.ExtensionFile;
import com.bitaspire.sir.Listener;
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
        if (main.getApi().getProcessorManager().isModernPipelineActive()) return;

        SIRUser user = main.getApi().getUserManager().getUser(event.getPlayer());
        if (user == null) return;

        ChatProcessor.Context context = new ChatProcessor.Context(user, event.getMessage(), event.isAsynchronous());
        process(context);
        event.setMessage(context.getMessage());
    }

    void process(ChatProcessor.Context context) {
        if (!file.get("enabled", true) ||
                main.getApi().getUserManager().hasPermission(
                        context.getPlayer(),
                        file.get("bypass", "sir.moderation.bypass.format")
                ))
            return;

        String message = context.getMessage();

        if (file.get("capitalize", false) && !message.isEmpty())
            message = Character.toUpperCase(message.charAt(0)) + message.substring(1);

        String charPath = "characters.";

        String prefix = file.get(charPath + "prefix", "");
        String suffix = file.get(charPath + "suffix", "");

        context.setMessage(prefix + message + suffix);
    }
}
