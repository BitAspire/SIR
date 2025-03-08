package me.croabeast.sir.plugin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.croabeast.sir.plugin.manager.WorldRuleManager;
import me.croabeast.sir.plugin.misc.WorldRule;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
final class RuleManagerImpl implements WorldRuleManager {

    private final Map<World, Map<WorldRule<?>, Object>> data = new HashMap<>();
    @Getter
    private boolean loaded = false;

    private final SIRPlugin plugin;

    @Override
    public void load() {
        if (loaded) return;

        loaded = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            for (World world : plugin.getServer().getWorlds()) {
                Map<WorldRule<?>, Object> map = data.getOrDefault(world, new HashMap<>());

                for (WorldRule<?> rule : WorldRule.values())
                    try {
                        Object value = rule.getValue(world);
                        if (value != null) map.put(rule, value);
                    }
                    catch (Exception ignored) {}

                data.put(world, map);
            }
        }) != -1;
    }

    @Override
    public void unload() {
        if (loaded) {
            data.clear();
            loaded = false;
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getLoadedValue(World world, WorldRule<T> rule) {
        Map<WorldRule<?>, Object> map = data.get(world);
        return map == null ? null : (T) map.get(rule);
    }
}
