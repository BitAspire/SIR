package me.croabeast.sir.command;

import com.google.common.base.Preconditions;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Getter
final class ProviderFile {

    private final String main;
    private final Map<String, ConfigurationSection> commands;

    ProviderFile(@NotNull YamlConfiguration configuration) {
        main = configuration.getString("main");
        Preconditions.checkArgument(StringUtils.isNotBlank(main), "Command provider main class cannot be null or empty");

        ConfigurationSection section = configuration.getConfigurationSection("commands");
        if (section == null) {
            commands = Collections.emptyMap();
            return;
        }

        Map<String, ConfigurationSection> map = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            if (StringUtils.isBlank(key)) continue;
            ConfigurationSection commandSection = section.getConfigurationSection(key);
            if (commandSection == null) continue;
            map.put(key.toLowerCase(Locale.ENGLISH), commandSection);
        }

        commands = Collections.unmodifiableMap(map);
    }

    boolean hasCommand(@Nullable String name) {
        return name != null && commands.containsKey(name.toLowerCase(Locale.ENGLISH));
    }

    ConfigurationSection getCommandSection(@Nullable String name) {
        if (name == null) return null;
        return commands.get(name.toLowerCase(Locale.ENGLISH));
    }
}
