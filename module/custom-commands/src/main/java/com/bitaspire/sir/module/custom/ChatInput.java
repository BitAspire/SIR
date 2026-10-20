package com.bitaspire.sir.module.custom;

import com.bitaspire.sir.Listener;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@RequiredArgsConstructor
final class ChatInput extends Listener {

    private final CustomCommands main;
    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();

    // The consumer gets null when the player cancels.
    void request(HumanEntity viewer, String field, String current, Consumer<String> onInput) {
        pending.put(viewer.getUniqueId(), onInput);
        viewer.closeInventory();

        main.getApi().getSender().setTargets(viewer).setLogger(false)
                .addPlaceholder("{field}", field)
                .addPlaceholder("{current}", current.isEmpty() ? "<empty>" : current)
                .send(main.lang.toStringList("lang.chat-input"));
    }

    boolean handle(Player player, String message) {
        Consumer<String> consumer = pending.remove(player.getUniqueId());
        if (consumer == null) return false;

        String text = message.trim();
        main.getApi().getScheduler().runTask(() -> consumer.accept(text.equalsIgnoreCase("cancel") ? null : text));
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onChat(AsyncPlayerChatEvent event) {
        if (!main.getApi().getProcessorManager().isModernPipelineActive() && handle(event.getPlayer(), event.getMessage()))
            event.setCancelled(true);
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }
}
