package me.croabeast.sir.plugin.hook;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public interface VaultHolder<T> {

    @NotNull
    T getSource();

    Plugin getPlugin();

    boolean isEnabled();

    default <V> V fromSource(Function<T, V> function) {
        return function.apply(getSource());
    }

    @Nullable
    String getPrimaryGroup(Player player);

    boolean isInGroup(Player player, String group);

    @NotNull
    List<String> getGroups(Player player);

    @Nullable
    String getPrefix(Player player);

    @Nullable
    String getSuffix(Player player);

    @Nullable
    String getGroupPrefix(World world, String group);

    @Nullable
    default String getGroupPrefix(String group) {
        return getGroupPrefix(null, group);
    }

    @Nullable
    String getGroupSuffix(World world, String group);

    @Nullable
    default String getGroupSuffix(String group) {
        return getGroupSuffix(null, group);
    }

    @NotNull
    List<String> getGroups();
}
