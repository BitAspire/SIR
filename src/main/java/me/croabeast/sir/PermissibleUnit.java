package me.croabeast.sir;

import me.croabeast.file.ConfigurableUnit;
import me.croabeast.sir.manager.UserManager;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * Represents a configuration unit used for handling permissions and groups in a configuration section.
 */
public interface PermissibleUnit extends ConfigurableUnit {

    /**
     * Checks if the given command sender has the permission associated with this unit.
     *
     * @param sender The command sender.
     * @return True if the sender has the permission, false otherwise.
     */
    default boolean hasPermission(CommandSender sender) {
        return UserManager.hasPermission(sender, getPermission());
    }

    /**
     * Checks if the given user has the permission associated with this unit.
     *
     * @param user The user.
     * @return True if the sender has the permission, false otherwise.
     */
    default boolean hasPermission(SIRUser user) {
        return UserManager.hasPermission(user, getPermission());
    }

    /**
     * Checks if the given command sender is in the group associated with this unit.
     *
     * @param sender The command sender.
     * @return True if the sender is in the group, false otherwise.
     */
    default boolean isInGroup(CommandSender sender) {
        Player player = sender instanceof Player ? (Player) sender : null;
        return player != null &&
                SIRPlugin.getInstance().getChat().isInGroup(player, getGroup());
    }

    /**
     * Creates a new ConfigUnit instance based on the provided configuration section.
     *
     * @param section The configuration section.
     *
     * @return A new ConfigUnit instance.
     * @throws NullPointerException If the configuration section is null.
     */
    static PermissibleUnit of(ConfigurationSection section) {
        return () -> Objects.requireNonNull(section);
    }

    /**
     * Creates a new ConfigUnit instance based on the provided ConfigUnit instance.
     *
     * @param unit The ConfigUnit instance.
     * @return A new ConfigUnit instance.
     */
    static PermissibleUnit of(PermissibleUnit unit) {
        return of(unit.getSection());
    }
}
