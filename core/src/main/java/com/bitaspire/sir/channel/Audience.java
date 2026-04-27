package com.bitaspire.sir.channel;

import me.croabeast.common.CollectionBuilder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public interface Audience {

    int getRadius();

    boolean isSameWorld();

    @Nullable
    String getPermission();

    @Nullable
    String getGroup();

    boolean shouldIncludeSender();

    @Nullable
    List<String> getWorldsNames();

    @NotNull
    default List<World> getWorlds() {
        List<String> list = getWorldsNames();

        if (list == null || list.isEmpty())
            return Bukkit.getWorlds();

        return CollectionBuilder.of(list)
                .map(Bukkit::getWorld)
                .filter(Objects::nonNull)
                .toList();
    }
}
