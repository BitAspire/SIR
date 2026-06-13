package com.bitaspire.sir;

import lombok.experimental.UtilityClass;
import me.leoko.advancedban.manager.PunishmentManager;
import me.leoko.advancedban.manager.UUIDManager;

@UtilityClass
class AdvancedBanUserHook {

    boolean isMuted(String name) {
        String id = UUIDManager.get().getUUID(name);
        return PunishmentManager.get().isMuted(id);
    }
}
