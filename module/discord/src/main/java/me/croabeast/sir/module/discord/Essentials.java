package me.croabeast.sir.module.discord;

import lombok.experimental.UtilityClass;
import me.croabeast.common.applier.StringApplier;
import me.croabeast.common.reflect.Reflector;
import net.essentialsx.api.v2.ChatType;
import net.essentialsx.api.v2.events.discord.DiscordChatMessageEvent;
import net.essentialsx.discord.EssentialsDiscord;
import net.essentialsx.discord.JDADiscordService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.UnaryOperator;

@UtilityClass
class Essentials {

    void send(Player player, String message, UnaryOperator<String> formatter) {
        EssentialsDiscord discord;
        try {
            discord = JavaPlugin.getPlugin(EssentialsDiscord.class);
        } catch (Exception e) {
            return;
        }

        JDADiscordService service = Reflector.from(() -> discord).get("jda");
        message = StringApplier.simplified(message).apply(formatter).toString();

        DiscordChatMessageEvent event = new DiscordChatMessageEvent(player, message, ChatType.UNKNOWN);
        Bukkit.getPluginManager().callEvent(event);

        service.sendChatMessage(player, event.getMessage());
    }
}
