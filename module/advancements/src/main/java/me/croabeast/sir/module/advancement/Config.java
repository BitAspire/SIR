package me.croabeast.sir.module.advancement;

import me.croabeast.file.Configurable;
import me.croabeast.sir.ExtensionFile;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class Config {

    private final Map<Type, Boolean> enabledMap = new HashMap<>(), whitelists = new HashMap<>();
    private final Map<Type, List<String>> values = new HashMap<>();

    private void loadEntries(Type type, ConfigurationSection section) {
        if (section == null) return;

        enabledMap.put(type, section.getBoolean("enabled", false));
        whitelists.put(type, section.getBoolean("whitelist", true));
        values.put(type, Configurable.toStringList(section, "list"));
    }

    Config(Advancements main) {
        try {
            ExtensionFile file = new ExtensionFile(main, "config", true);

            loadEntries(Type.GAME_MODE, file.getSection("game-modes"));
            loadEntries(Type.WORLD, file.getSection("worlds"));
            loadEntries(Type.ADVANCEMENT, file.getSection("advancements"));
        }
        catch (Exception ignored) {}
    }

    boolean isProhibited(Type type, String value) {
        if (!enabledMap.getOrDefault(type, false))
            return false;

        List<String> list = values.get(type);
        if (list == null || list.isEmpty()) return false;

        boolean contains = list.contains(value);
        return whitelists.getOrDefault(type, true) != contains;
    }

    enum Type {
        GAME_MODE,
        WORLD,
        ADVANCEMENT
    }
}
