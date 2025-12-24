package me.croabeast.sir.module.cooldown;

import lombok.SneakyThrows;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.PermissibleUnit;
import me.croabeast.sir.user.SIRUser;

import java.util.ArrayList;
import java.util.List;

final class Data {

    private final List<CooldownUnit> units = new ArrayList<>();

    @SneakyThrows
    Data(Cooldowns main) {
        ExtensionFile file = new ExtensionFile(main, "cooldowns", true);
        units.addAll(PermissibleUnit.loadUnits(file.getSection("cooldowns"), CooldownUnit::new));
    }

    CooldownUnit getUnit(SIRUser user) {
        return PermissibleUnit.getUnit(user, units, false);
    }
}
