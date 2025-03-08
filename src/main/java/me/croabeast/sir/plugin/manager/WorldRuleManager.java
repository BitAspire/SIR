package me.croabeast.sir.plugin.manager;

import me.croabeast.lib.Loadable;
import me.croabeast.sir.plugin.misc.WorldRule;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

public interface WorldRuleManager extends Loadable {

    @Nullable
    <T> T getLoadedValue(World world, WorldRule<T> rule);
}
