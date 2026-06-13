package com.bitaspire.sir;

import com.Zrips.CMI.Containers.CMIUser;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;

@SuppressWarnings("ConstantValue")
@UtilityClass
class CmiUserHook {

    boolean isMuted(Player player) {
        CMIUser user = CMIUser.getUser(player);
        return user != null && user.isMuted();
    }

    boolean isVanished(Player player) {
        CMIUser user = CMIUser.getUser(player);
        return user != null && user.isVanished();
    }
}
