package me.croabeast.sir.module;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface PlayerFormatter<T> {

    String format(Player player, String string, T reference);

    default String format(Player player, String string) {
        return format(player, string, null);
    }
}
