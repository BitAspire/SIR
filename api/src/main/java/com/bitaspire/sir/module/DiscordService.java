package com.bitaspire.sir.module;

import org.bukkit.entity.Player;

import java.util.function.UnaryOperator;

/**
 * Provides Discord integration for SIR modules.
 *
 * <p> Implementations bridge in-game events to a Discord webhook or bot,
 * allowing messages to be forwarded to a specified channel.
 */
public interface DiscordService {

    /**
     * Sends a message to the specified Discord channel on behalf of the given player.
     *
     * @param channel the Discord channel identifier (name or ID).
     * @param player the player whose context is used for message formatting.
     * @param operator a string transformer applied to the message before sending.
     */
    void sendMessage(String channel, Player player, UnaryOperator<String> operator);

    /**
     * Returns whether this service is operating in restricted mode.
     *
     * <p> In restricted mode, certain message types or channels may be blocked.
     *
     * @return {@code true} if restricted.
     */
    boolean isRestricted();
}
