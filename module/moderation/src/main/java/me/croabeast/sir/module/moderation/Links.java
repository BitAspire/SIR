package me.croabeast.sir.module.moderation;

import me.croabeast.takion.chat.ChatComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Links extends Module {

    Links(Moderation main) {
        super(main, "links");
    }

    @Override
    boolean processCancellation(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();

        List<String> links = file.toStringList("allowed-links");
        boolean foundAny = false;

        Matcher matcher = ChatComponent.URL_PATTERN.matcher(message);
        List<String> restrictedLinks = new ArrayList<>();

        while (matcher.find()) {
            String match = matcher.group();
            boolean allowed = false;

            for (String link : links)
                if (match.matches("(?i)" + Pattern.quote(link))) {
                    allowed = true;
                    break;
                }

            if (allowed) continue;

            restrictedLinks.add(match);
            foundAny = true;
        }

        if (foundAny) {
            validateAndExecuteActions(
                    player, message,
                    file.get("actions.maximum-violations", 3)
            );

            if (file.get("control", "BLOCK").matches("(?i)block"))
                return true;

            List<String> list = file.toStringList("replace-options.replacements");

            for (String link : restrictedLinks) {
                String replace = getReplacement(list, link);
                message = message.replace(link, replace);
            }

            event.setMessage(message);
        }

        return false;
    }
}
