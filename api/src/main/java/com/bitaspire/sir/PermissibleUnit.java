package com.bitaspire.sir;

import me.croabeast.file.ConfigurableUnit;
import com.bitaspire.sir.user.SIRUser;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

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
        return isDefaultPermission() || SIRApi.instance().getUserManager().hasPermission(sender, getPermission());
    }

    /**
     * Checks if the given user has the permission associated with this unit.
     *
     * @param user The user.
     * @return True if the sender has the permission, false otherwise.
     */
    default boolean hasPermission(SIRUser user) {
        return isDefaultPermission() || SIRApi.instance().getUserManager().hasPermission(user, getPermission());
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
                SIRApi.instance().getChat().getPermissionProvider().isInGroup(player, getGroup());
    }

    /**
     * Checks if the given user is in the group associated with this unit.
     *
     * @param user the user.
     * @return {@code true} if the user is in the group, {@code false} otherwise.
     */
    default boolean isInGroup(SIRUser user) {
        return user != null && isInGroup(user.isOnline() ? user.getPlayer() : null);
    }

    /**
     * Returns whether this unit uses the default permission (empty or {@code "DEFAULT"}).
     *
     * @return {@code true} if the permission is the default.
     */
    default boolean isDefaultPermission() {
        String temp = getPermission().trim();
        return temp.isEmpty() || "DEFAULT".equalsIgnoreCase(temp);
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

    /**
     * Loads a list of units from a configuration section using the given factory function.
     * Units are sorted by the default order before being returned.
     *
     * @param section the parent configuration section.
     * @param function factory that converts a child section into a unit.
     * @param <U> the unit type.
     * @return sorted list of units.
     */
    static <U extends PermissibleUnit> List<U> loadUnits(ConfigurationSection section, Function<ConfigurationSection, U> function) {
        return UnitUtils.loadUnits(section, function);
    }

    /**
     * Loads a list of plain {@link PermissibleUnit} instances from a configuration section.
     *
     * @param section the parent configuration section.
     * @return sorted list of units.
     */
    static List<PermissibleUnit> loadUnits(ConfigurationSection section) {
        return loadUnits(section, PermissibleUnit::of);
    }

    /**
     * Finds the first unit from the collection that matches the user's permissions and group.
     *
     * @param user the user to match against.
     * @param units the candidate units.
     * @param order if {@code true}, units are sorted by the default order before matching.
     * @param <U> the unit type.
     * @return the first matching unit, or {@code null} if none match.
     */
    static <U extends PermissibleUnit> U getUnit(SIRUser user, Collection<U> units, boolean order) {
        return UnitUtils.getUnit(user, units, order);
    }

    /**
     * Returns the default comparator used to order units (by priority, group, and name).
     *
     * @return the default order comparator.
     */
    static Comparator<PermissibleUnit> getDefaultOrder() {
        return UnitUtils.ORDER;
    }
}
