package me.croabeast.sir.plugin.misc;

import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.manager.SIRUserManager;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public interface SIRUser {

    @NotNull
    OfflinePlayer getOfflinePlayer();

    @NotNull
    Player getPlayer();

    @NotNull
    String getName();

    @NotNull
    UUID getUuid();

    @Nullable
    default String getPrefix() {
        return SIRPlugin.getInstance().getVaultHolder().getPrefix(getPlayer());
    }

    @Nullable
    default String getSuffix() {
        return SIRPlugin.getInstance().getVaultHolder().getSuffix(getPlayer());
    }

    default boolean hasPerm(String perm) {
        return SIRUserManager.hasPerm(getPlayer(), perm);
    }

    default boolean isOnline() {
        return getOfflinePlayer().isOnline();
    };

    boolean isLogged();

    void setLogged(boolean logged);

    boolean isVanished();

    boolean isImmune();

    void giveImmunity(int seconds);

    void ignore(SIRUser user, boolean isChat);

    void ignore(Player player, boolean isChat);

    void ignoreAll(boolean isChat);

    void unignore(SIRUser user, boolean isChat);

    void unignore(Player player, boolean isChat);

    void unignoreAll(boolean isChat);

    boolean isIgnoring(SIRUser user, boolean isChat);

    boolean isIgnoring(Player player, boolean isChat);

    boolean isIgnoringAll(boolean isChat);

    boolean isLocalChannelToggled(String channel);

    boolean isMuted();

    long getRemainingMute();

    default void teleport(ConfigurationSection id) {
        if (id == null || id.getBoolean("enabled"))
            return;

        World world = Bukkit.getWorld(id.getString("world", ""));
        if (world == null) return;

        Location loc = world.getSpawnLocation();

        String coords = id.getString("coordinates");
        String rot = id.getString("rotation");

        if (coords != null) {
            final String[] mC = coords.split(",", 3);

            try {
                loc.setX(Double.parseDouble(mC[0]));
            } catch (Exception ignored) {}
            try {
                loc.setY(Double.parseDouble(mC[1]));
            } catch (Exception ignored) {}
            try {
                loc.setZ(Double.parseDouble(mC[2]));
            } catch (Exception ignored) {}
        }

        if (rot != null) {
            final String[] mD = rot.split(",", 2);

            try {
                loc.setYaw(Float.parseFloat(mD[0]));
            } catch (Exception ignored) {}
            try {
                loc.setPitch(Float.parseFloat(mD[1]));
            } catch (Exception ignored) {}
        }

        getPlayer().teleport(loc);
    }

    default void playSound(String rawSound) {
        Sound sound;
        try {
            sound = Sound.valueOf(rawSound);
        } catch (Exception e) {
            return;
        }

        final Player p = getPlayer();
        p.playSound(p.getLocation(), sound, 1, 1);
    }

    @NotNull
    Set<SIRUser> getNearbyUsers(double range);
}
