package me.croabeast.sir.manager;

import me.croabeast.sir.FileData;
import me.croabeast.sir.user.SIRUser;
import org.apache.commons.lang.StringUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
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
    Set<SIRUser> getOfflineUsers();

    @NotNull
    Set<SIRUser> getOnlineUsers();

    static boolean hasPermission(CommandSender sender, String permission) {
        if (StringUtils.isBlank(permission)) return false;
        if (sender instanceof ConsoleCommandSender || permission.matches("(?i)DEFAULT"))
            return true;

        boolean hasPermission = sender.hasPermission(permission);
        if (!FileData.Main.CONFIG.getFile().get("options.override-op", false))
            return hasPermission;

        return (!sender.isOp() || sender.isPermissionSet(permission)) && hasPermission;
    }

    static boolean hasPermission(SIRUser user, String permission) {
        return hasPermission(user.getPlayer(), permission);
    }

    /**
     * Saves all user data safely without removing users from the map.
     * This is safe to call during reload operations.
     */
    void saveAllDataSafely();

    /**
     * Starts the auto-save task if enabled in config.
     */
    void startAutoSave();

    /**
     * Stops the auto-save task if running.
     */
    void stopAutoSave();

    /**
     * Gets the Runnable for safe reload operations.
     * @return Runnable that saves all user data safely
     */
    default Runnable getSaveRunnable() {
        return this::saveAllDataSafely;
    }
}
