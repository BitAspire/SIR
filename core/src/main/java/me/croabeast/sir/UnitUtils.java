package me.croabeast.sir;

import lombok.experimental.UtilityClass;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@UtilityClass
class UnitUtils {

    final Comparator<PermissibleUnit> ORDER = Comparator
            .comparing((PermissibleUnit u) -> u.isDefaultPermission() ? 1 : 0)
            .thenComparing(s -> s.getGroup() != null ? 0 : 1)
            .thenComparing(PermissibleUnit::getPriority, Comparator.reverseOrder())
            .thenComparing(PermissibleUnit::getName, String.CASE_INSENSITIVE_ORDER);

    <U extends PermissibleUnit> List<U> loadUnits(ConfigurationSection section, Function<ConfigurationSection, U> function) {
        if (section == null || function == null)
            return new ArrayList<>();

        List<U> units = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s != null) units.add(function.apply(s));
        }

        units.sort(ORDER);
        return units;
    }

    <U extends PermissibleUnit> U getUnit(SIRUser user, Collection<U> units, boolean order) {
        if (user == null || units == null || units.isEmpty()) return null;

        Stream<U> stream = units.stream();
        if (order) stream = stream.sorted(ORDER);

        return stream
                .filter(s -> s.getGroup() == null || s.isInGroup(user))
                .filter(u -> u.hasPermission(user)).findFirst().orElse(null);
    }
}
