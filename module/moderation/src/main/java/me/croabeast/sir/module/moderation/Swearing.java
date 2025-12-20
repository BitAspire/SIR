package me.croabeast.sir.module.moderation;

import me.croabeast.common.CollectionBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;
import java.util.regex.Matcher;

final class Swearing extends Module {

    Swearing(Moderation main) {
        super(main, "swearing");
    }

    @Override
    boolean processCancellation(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();

        List<RegexLine> lines = CollectionBuilder.of(file.toStringList("banned-words")).map(RegexLine::new).toList();

        boolean foundAny = false;
        boolean block = file.get("control", "BLOCK").matches("(?i)block");

        for (RegexLine line : lines) {
            Matcher matcher = line.matcher(message);

            if (block && matcher.find()) {
                foundAny = true;
                break;
            }

            while (matcher.find()) {
                List<String> list = file.toStringList("replace-options.replacements");

                String group = matcher.group();
                String replace = getReplacement(list, group);

                if (!foundAny) foundAny = true;
                event.setMessage(message = message.replace(group, replace));
            }
        }

        return foundAny && validateAndExecuteActions(player, message, file.get("actions.maximum-violations", 3));
    }
}
