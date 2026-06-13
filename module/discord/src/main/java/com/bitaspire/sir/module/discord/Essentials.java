package com.bitaspire.sir.module.discord;

import lombok.experimental.UtilityClass;
import me.croabeast.common.applier.StringApplier;
import net.essentialsx.api.v2.ChatType;
import net.essentialsx.api.v2.events.discord.DiscordChatMessageEvent;
import net.essentialsx.api.v2.services.discord.DiscordService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.function.UnaryOperator;

@UtilityClass
class Essentials {

    void send(Player player, String message, UnaryOperator<String> formatter) {
        RegisteredServiceProvider<DiscordService> registration =
                Bukkit.getServicesManager().getRegistration(DiscordService.class);
        if (registration == null) return;

        DiscordService service = registration.getProvider();
        if (service == null) return;

        message = StringApplier.simplified(message).apply(formatter).toString();

        DiscordChatMessageEvent event = new DiscordChatMessageEvent(player, message, ChatType.UNKNOWN);
        Bukkit.getPluginManager().callEvent(event);

        service.sendChatMessage(player, event.getMessage());
    }
}
