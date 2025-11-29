package me.croabeast.sir.command;

import lombok.Getter;
import me.croabeast.file.Configurable;
import me.croabeast.sir.SIRApi;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
final class CommandFile {

    private final String name, permission;
    private final boolean override, enabled;

    private final List<String> aliases;
    private final Map<String, String> subCommands;

    private final String description, usage;
    private final String permissionMessage;

    final boolean parent;
    final String parentName;

    CommandFile(ConfigurationSection section) {
        Objects.requireNonNull(section, "Configuration section cannot be null");

        name = section.getString("name");
        permission = section.getString("permission", "sir." + name);

        Objects.requireNonNull(name, "Command name cannot be null");
        Objects.requireNonNull(permission, "Command permission cannot be null");

        aliases = Configurable.toStringList(section, "aliases");

        Map<String, String> subCommands = new LinkedHashMap<>();

        ConfigurationSection sub = section.getConfigurationSection("sub-commands");
        if (sub != null) for (String key : sub.getKeys(false))
            subCommands.put(key, sub.getString(key, permission + "." + key));

        this.subCommands = new LinkedHashMap<>(subCommands);

        this.description = section.getString("description", "");
        this.usage = section.getString("usage", "/" + name);
        this.permissionMessage = section.getString("lang.permission-message", "You do not have permission to execute this command.");

        boolean e = section.getBoolean("enabled");
        boolean o = section.getBoolean("override-existing");

        parent = section.getBoolean("depends.enabled");
        parentName = section.getString("depends.parent");

        final SIRCommand parent;
        if (hasParent() &&
                (parent = SIRApi.instance().getCommandManager().getCommand(parentName)) != null &&
                parent.hasParent())
        {
            e = parent.isEnabled();
            o = parent.isOverriding();
        }

        this.enabled = e;
        this.override = o;
    }

    boolean hasParent() {
        return parent && StringUtils.isNotBlank(parentName);
    }
}
