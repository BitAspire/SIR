package com.bitaspire.sir.module.custom;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

// Values written by [save:...] lines. Saved on every change, since they are rare and must survive crashes.
@RequiredArgsConstructor
final class DataStore {

    private final CustomCommands main;
    private YamlConfiguration yml = new YamlConfiguration();

    private File getFile() {
        return new File(main.getDataFolder(), "data.yml");
    }

    // A dot would nest the key in the YAML file.
    private static String path(@Nullable UUID uuid, String key) {
        return (uuid == null ? "global." : "players." + uuid + ".") + key.replace('.', '_');
    }

    void load() {
        yml = YamlConfiguration.loadConfiguration(getFile());
    }

    // Empty when nothing was saved.
    String get(@Nullable UUID uuid, String key) {
        return yml.getString(path(uuid, key), "");
    }

    // Keys saved for the server, or for that player.
    Set<String> keys(@Nullable UUID uuid) {
        ConfigurationSection section = yml.getConfigurationSection(uuid == null ? "global" : "players." + uuid);
        return section == null ? Collections.emptySet() : section.getKeys(false);
    }

    // An empty value removes the key.
    @SneakyThrows
    void set(@Nullable UUID uuid, String key, String value) {
        yml.set(path(uuid, key), StringUtils.isBlank(value) ? null : value);
        yml.save(getFile());
    }
}
