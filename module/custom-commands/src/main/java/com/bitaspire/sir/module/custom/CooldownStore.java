package com.bitaspire.sir.module.custom;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Kept by the module, so editing or reloading a command keeps its cooldowns; saved on unregister.
@RequiredArgsConstructor
final class CooldownStore {

    // The key of 'global-cooldown', shared by every player.
    static final UUID GLOBAL = new UUID(0, 0);

    private final CustomCommands main;
    private final Map<String, Map<UUID, Long>> expiries = new HashMap<>();

    private File getFile() {
        return new File(main.getDataFolder(), "cooldowns.yml");
    }

    long getRemaining(String command, UUID uuid) {
        Map<UUID, Long> map = expiries.get(command);
        return map == null ? 0 : map.getOrDefault(uuid, 0L) - System.currentTimeMillis();
    }

    void start(String command, UUID uuid, long millis) {
        expiries.computeIfAbsent(command, k -> new HashMap<>()).put(uuid, System.currentTimeMillis() + millis);
    }

    void load() {
        expiries.clear();

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(getFile());
        for (String command : yml.getKeys(false)) {
            ConfigurationSection section = yml.getConfigurationSection(command);
            if (section == null) continue;

            for (String key : section.getKeys(false))
                try {
                    start(command, UUID.fromString(key), section.getLong(key) - System.currentTimeMillis());
                } catch (IllegalArgumentException ignored) {}
        }
    }

    @SneakyThrows
    void save() {
        long now = System.currentTimeMillis();
        YamlConfiguration yml = new YamlConfiguration();

        expiries.forEach((command, map) -> map.forEach((uuid, expiry) -> {
            if (expiry > now) yml.set(command + "." + uuid, expiry);
        }));
        yml.save(getFile());
    }
}
