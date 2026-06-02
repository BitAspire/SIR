package com.bitaspire.sir.command;

import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.croabeast.common.util.ArrayUtils;
import me.croabeast.file.Configurable;
import com.bitaspire.sir.Information;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Metadata descriptor for a {@link StandaloneProvider}, parsed from its bundled {@code commands.yml}.
 *
 * <p> Holds the provider's identity (main class, name, title, description) and the
 * raw configuration sections for each declared command.
 */
@Getter
public final class ProviderInformation implements Information {

    /** Mapping of lowercase command keys to their raw configuration sections. */
    private final Map<String, ConfigurationSection> commands;

    /** Fully-qualified main class name of the provider. */
    private final String main;

    /** Internal identifier name derived from the configuration or the main class. */
    private final String name;

    /** Human-readable display title shown in the GUI. */
    private final String title;

    /** Description lines shown in the GUI item lore. */
    private final String[] description;

    /** GUI slot position; set by the manager after all providers are loaded. */
    @Setter(AccessLevel.PACKAGE)
    private Slot slot = Slot.fromXY(0, 0);

    @ApiStatus.Internal
    public ProviderInformation(@NotNull YamlConfiguration configuration) {
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

    @ApiStatus.Internal
    public boolean hasNoCommand(@Nullable String name) {
        return name == null || !commands.containsKey(name.toLowerCase(Locale.ENGLISH));
    }

    @ApiStatus.Internal
    public ConfigurationSection getCommandSection(@Nullable String name) {
        return name == null ? null : commands.get(name.toLowerCase(Locale.ENGLISH));
    }
}
