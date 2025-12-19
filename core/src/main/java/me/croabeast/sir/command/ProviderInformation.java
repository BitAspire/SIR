package me.croabeast.sir.command;

import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import me.croabeast.common.util.ArrayUtils;
import me.croabeast.file.Configurable;
import me.croabeast.sir.Information;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Getter
public final class ProviderInformation implements Information {

    private final Map<String, ConfigurationSection> commands;

    private final String main, name, title;
    private final String[] description;

    @Setter
    private Slot slot;

    ProviderInformation(@NotNull YamlConfiguration configuration) {
        main = configuration.getString("main");
        Preconditions.checkArgument(StringUtils.isNotBlank(main), "Command provider main class cannot be null or empty");

        String[] mainParts = main.split("\\.");
        this.name = configuration.getString("name", mainParts[mainParts.length - 1]);

        List<String> list = ArrayUtils.toList(name.replaceAll("[-_]", " ").split(" "));
        list.replaceAll(WordUtils::capitalize);
        this.title = configuration.getString("title", String.join(" ", list));

        this.description = Configurable.toStringList(configuration, "description").toArray(new String[0]);

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
        return name == null ? null : commands.get(name.toLowerCase(Locale.ENGLISH));
    }
}
