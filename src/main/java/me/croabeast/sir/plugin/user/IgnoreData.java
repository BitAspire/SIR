package me.croabeast.sir.plugin.user;

import org.bukkit.entity.Player;

public interface IgnoreData {

    boolean isIgnoring(SIRUser player, boolean chat);

    boolean isIgnoring(Player player, boolean chat);

    boolean isIgnoringAll(boolean chat);

    void ignore(SIRUser user, boolean chat);

    void ignore(Player player, boolean chat);

    void ignoreAll(boolean chat);

    void unignore(SIRUser user, boolean chat);

    void unignore(Player player, boolean chat);

    void unignoreAll(boolean chat);
}
