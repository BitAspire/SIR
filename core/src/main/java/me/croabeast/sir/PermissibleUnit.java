package me.croabeast.sir;

import me.croabeast.file.ConfigurableUnit;
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
        return SIRApi.instance().getUserManager().hasPermission(sender, getPermission());
    }

    /**
     * Checks if the given user has the permission associated with this unit.
     *
     * @param user The user.
     * @return True if the sender has the permission, false otherwise.
     */
    default boolean hasPermission(SIRUser user) {
        return SIRApi.instance().getUserManager().hasPermission(user, getPermission());
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
                SIRApi.instance().getChat().isInGroup(player, getGroup());
    }

    /**
     * Creates a new PermissibleUnit instance based on the provided configuration section.
     *
     * @param section The configuration section.
     *
     * @return A new PermissibleUnit instance.
     * @throws NullPointerException If the configuration section is null.
     */
    static PermissibleUnit of(ConfigurationSection section) {
        return () -> Objects.requireNonNull(section);
    }

    /**
     * Creates a new PermissibleUnit instance based on the provided PermissibleUnit instance.
     *
     * @param unit The PermissibleUnit instance.
     * @return A new PermissibleUnit instance.
     */
    static PermissibleUnit of(PermissibleUnit unit) {
        return of(unit.getSection());
    }
}
