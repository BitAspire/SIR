package com.bitaspire.sir.module.scoreboard;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.croabeast.common.util.ReplaceUtils;
import me.croabeast.prismatic.PrismaticAPI;
import org.bukkit.entity.Player;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ScoreboardPlaceholders {

    private static final String[] KEYS = {
            "{player}", "{display_name}", "{world}", "{online}", "{max_online}", "{ping}", "{tps}"
    };

    static String apply(ScoreboardModule module, Player player, ScoreboardRefreshContext context, String value) {
        String text = ReplaceUtils.replaceEach(KEYS, values(player, context), value == null ? "" : value);
        try {
            return module.getApi().getLibrary().colorize(player, text);
        } catch (Exception ignored) {
            return PrismaticAPI.colorize(text);
        }
    }

    private static Object[] values(Player player, ScoreboardRefreshContext context) {
        return new Object[] {
                player.getName(),
                player.getDisplayName(),
                player.getWorld().getName(),
                context.getOnline(),
                context.getMaxOnline(),
                context.ping(player),
                context.getTps()
        };
    }
}
