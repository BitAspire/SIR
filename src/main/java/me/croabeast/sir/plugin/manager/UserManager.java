package me.croabeast.sir.plugin.manager;

import me.croabeast.sir.plugin.FileData;
import me.croabeast.sir.plugin.misc.SIRUser;
import org.apache.commons.lang.StringUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

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

    void mute(SIRUser user, int seconds, String admin, String reason);

    default void mute(SIRUser user, int seconds) {
        mute(user, seconds, "", "");
    }

    void unmute(SIRUser user);

    default void mute(OfflinePlayer player, int seconds, String admin, String reason) {
        mute(getUser(player), seconds, admin, reason);
    }

    default void mute(OfflinePlayer player, int seconds) {
        mute(getUser(player), seconds);
    }

    default void unmute(OfflinePlayer player) {
        unmute(getUser(player));
    }

    default void mute(Player player, int seconds, String admin, String reason) {
        mute(getUser(player), seconds, admin, reason);
    }

    default void mute(Player player, int seconds) {
        mute(getUser(player), seconds);
    }

    default void unmute(Player player) {
        unmute(getUser(player));
    }

    void toggleLocalChannelView(SIRUser user, String channel);

    default void toggleLocalChannelView(Player player, String channel) {
        toggleLocalChannelView(getUser(player), channel);
    }

    Set<SIRUser> getOfflineUsers();

    Set<SIRUser> getOnlineUsers();

    static boolean hasPerm(CommandSender sender, String perm) {
        if (StringUtils.isBlank(perm)) return false;
        if (sender instanceof ConsoleCommandSender || perm.matches("(?i)DEFAULT"))
            return true;

        boolean hasPermission = sender.hasPermission(perm);
        if (!FileData.Main.CONFIG.getFile().get("options.override-op", false))
            return hasPermission;

        return (!sender.isOp() || sender.isPermissionSet(perm)) && hasPermission;
    }

    static boolean hasPerm(SIRUser user, String perm) {
        return hasPerm(user.getPlayer(), perm);
    }
}
