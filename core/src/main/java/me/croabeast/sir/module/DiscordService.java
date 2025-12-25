package me.croabeast.sir.module;

import org.bukkit.entity.Player;

import java.util.function.UnaryOperator;

public interface DiscordService {

    void sendMessage(String channel, Player player, UnaryOperator<String> operator);

    boolean isRestricted();
}
