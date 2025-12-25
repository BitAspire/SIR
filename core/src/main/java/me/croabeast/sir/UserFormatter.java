package me.croabeast.sir;

import me.croabeast.sir.user.SIRUser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface UserFormatter<T> {

    @NotNull
    String format(SIRUser user, String string, T reference);

    @NotNull
    default String format(SIRUser user, String string) {
        return format(user, string, null);
    }

    @NotNull
    default String format(Player player, String string, T reference) {
        return format(SIRApi.instance().getUserManager().getUser(player), string, reference);
    }

    @NotNull
    default String format(Player player, String string) {
        return format(SIRApi.instance().getUserManager().getUser(player), string, null);
    }
}
