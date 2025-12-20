package me.croabeast.sir.module.moderation;

import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Locale;

final class Caps extends Module {

    Caps(Moderation main) {
        super(main, "caps");
    }

    private int longestConsecutiveUppercase(String msg) {
        int maxConsecutive = 0;
        int currentCount = 0;

        for (final char ch : msg.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                currentCount++;
                if (currentCount > maxConsecutive)
                    maxConsecutive = currentCount;

                continue;
            }
            currentCount = 0;
        }

        return maxConsecutive;
    }

    @Override
    boolean processCancellation(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();

        int capsCount = longestConsecutiveUppercase(message);

        int max = file.get("maximum-caps", 10);
        if (capsCount <= max) return false;

        validateAndExecuteActions(player, message, max);
        if (file.get("control", "BLOCK").matches("(?i)block"))
            return true;

        event.setMessage(message.toLowerCase(Locale.ENGLISH));
        return false;
    }
}
