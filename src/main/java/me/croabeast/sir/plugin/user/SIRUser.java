package me.croabeast.sir.plugin.user;

import me.croabeast.sir.plugin.manager.UserManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public interface SIRUser {

    @NotNull
    OfflinePlayer getOffline();

    @NotNull
    Player getPlayer();

    @NotNull
    String getName();

    @NotNull
    UUID getUuid();

    @Nullable
    String getPrefix();

    @Nullable
    String getSuffix();

    default boolean hasPermission(String permission) {
        return UserManager.hasPermission(this, permission);
    }

    default boolean isOnline() {
        return getOffline().isOnline();
    }

    boolean isLogged();

    void setLogged(boolean logged);

    boolean isVanished();

    @NotNull
    IgnoreData getIgnoreData();

    @NotNull
    MuteData getMuteData();

    @NotNull
    ChannelData getChannelData();

    @NotNull
    ImmuneData getImmuneData();

    default void playSound(String rawSound, float volume, float pitch) {
        Sound sound;
        try {
            sound = Sound.valueOf(rawSound);
        } catch (Exception e) {
            return;
        }

        final Player p = getPlayer();
        p.playSound(p.getLocation(), sound, volume, pitch);
    }

    default void playSound(String sound) {
        playSound(sound, 1.0f, 1.0f);
    }

    @NotNull
    Set<SIRUser> getNearbyUsers(double range);
}
