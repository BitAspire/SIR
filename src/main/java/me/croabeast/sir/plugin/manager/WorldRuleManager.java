package me.croabeast.sir.plugin.manager;

import me.croabeast.common.Loadable;
import me.croabeast.common.WorldRule;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

public interface WorldRuleManager extends Loadable {

    @Nullable
    <T> T getLoadedValue(World world, WorldRule<T> rule);
}
