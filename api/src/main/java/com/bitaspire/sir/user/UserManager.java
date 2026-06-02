package com.bitaspire.sir.user;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

/**
 * Manages {@link SIRUser} instances for all players known to the server.
 *
 * <p> Provides lookup, iteration, and permission-checking utilities. Users are
 * tracked for both online and offline players.
 */
public interface UserManager {

    /**
     * Returns the user with the given UUID, or {@code null} if not found.
     *
     * @param uuid the player UUID.
     * @return the user, or {@code null}.
     */
    SIRUser getUser(UUID uuid);

    /**
     * Returns the user for the given offline player, or {@code null} if not found.
     *
     * @param player the offline player.
     * @return the user, or {@code null}.
     */
    default SIRUser getUser(OfflinePlayer player) {
        return player == null ? null : getUser(player.getUniqueId());
    }

    /**
     * Returns the user for the given online player, or {@code null} if not found.
     *
     * @param player the online player.
     * @return the user, or {@code null}.
     */
    default SIRUser getUser(Player player) {
        return player == null ? null : getUser(player.getUniqueId());
    }

    /**
     * Returns the user for the given command sender if they are a player, or {@code null} otherwise.
     *
     * @param sender the command sender.
     * @return the user, or {@code null} if the sender is not a player.
     */
    default SIRUser getUser(CommandSender sender) {
        return sender instanceof Player ? getUser((Player) sender) : null;
    }

    /**
     * Returns the user with the given exact name, or {@code null} if not found.
     *
     * @param name the player name.
     * @return the user, or {@code null}.
     */
    SIRUser getUser(String name);

    /**
     * Returns the user whose name most closely matches the given string, or {@code null} if not found.
     *
     * <p> Useful for partial name lookups from command arguments.
     *
     * @param name the partial or full player name.
     * @return the closest matching user, or {@code null}.
     */
    SIRUser fromClosest(String name);

    /**
     * Returns all known users, optionally filtered to online players only.
     *
     * @param online if {@code true}, returns only currently online users.
     * @return the user set.
     */
    @NotNull
    Set<SIRUser> getUsers(boolean online);

    /**
     * Returns all currently online users.
     * Equivalent to {@link #getUsers(boolean) getUsers(true)}.
     *
     * @return the online user set.
     */
    @NotNull
    default Set<SIRUser> getUsers() {
        return getUsers(true);
    }

    /**
     * Returns whether the given command sender has the specified permission.
     *
     * @param sender the command sender.
     * @param permission the permission node to check.
     * @return {@code true} if the sender has the permission.
     */
    boolean hasPermission(CommandSender sender, String permission);

    /**
     * Returns whether the given user has the specified permission.
     *
     * @param user the user.
     * @param permission the permission node to check.
     * @return {@code true} if the user has the permission.
     */
    default boolean hasPermission(SIRUser user, String permission) {
        return hasPermission(user.getPlayer(), permission);
    }
}
