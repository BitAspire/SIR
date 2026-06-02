package com.bitaspire.sir.channel;

import me.croabeast.common.CollectionBuilder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Defines who receives messages sent in a {@link ChatChannel}.
 *
 * <p> Audience scope can be restricted by radius, world, permission, or group.
 */
public interface Audience {

    /**
     * Returns the block radius within which players receive messages.
     * A value of {@code 0} or less means no radius restriction.
     *
     * @return the radius in blocks.
     */
    int getRadius();

    /**
     * Returns whether only players in the same world as the sender receive messages.
     *
     * @return {@code true} if same-world restriction is active.
     */
    boolean isSameWorld();

    /**
     * Returns the permission node required to receive messages in this channel.
     *
     * @return the permission string, or {@code null} if unrestricted.
     */
    @Nullable
    String getPermission();

    /**
     * Returns the permission group required to receive messages in this channel.
     *
     * @return the group name, or {@code null} if unrestricted.
     */
    @Nullable
    String getGroup();

    /**
     * Returns whether the sender also receives their own message.
     *
     * @return {@code true} if the sender is included in the audience.
     */
    boolean shouldIncludeSender();

    /**
     * Returns the list of world names that restrict this channel's audience.
     *
     * @return world names, or {@code null} / empty to include all worlds.
     */
    @Nullable
    List<String> getWorldsNames();

    /**
     * Returns the resolved {@link World} instances from {@link #getWorldsNames()}.
     *
     * <p> Falls back to all server worlds if the world name list is null or empty.
     *
     * @return the list of worlds.
     */
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
