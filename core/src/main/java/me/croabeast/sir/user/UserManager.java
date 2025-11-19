package me.croabeast.sir.user;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

public interface UserManager {

    SIRUser getUser(UUID uuid);

    default SIRUser getUser(OfflinePlayer player) {
        return player == null ? null : getUser(player.getUniqueId());
    }

    default SIRUser getUser(Player player) {
        return player == null ? null : getUser(player.getUniqueId());
    }

    default SIRUser getUser(CommandSender sender) {
        return sender instanceof Player ? getUser((Player) sender) : null;
    }

    SIRUser getUser(String name);

    SIRUser fromClosest(String name);

    @NotNull
    Set<SIRUser> getUsers(boolean online);

    @NotNull
    default Set<SIRUser> getUsers() {
        return getUsers(true);
    }

    boolean hasPermission(CommandSender sender, String permission);

    default boolean hasPermission(SIRUser user, String permission) {
        return hasPermission(user.getPlayer(), permission);
    }
}
