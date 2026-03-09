package com.bitaspire.sir.module.cooldown;

import lombok.SneakyThrows;
import com.bitaspire.sir.ExtensionFile;
import com.bitaspire.sir.PermissibleUnit;
import com.bitaspire.sir.user.SIRUser;

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
