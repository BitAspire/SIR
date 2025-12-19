package me.croabeast.sir.command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.croabeast.file.Configurable;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
public final class CommandFile {

    private final String name, permission;
    @Setter(AccessLevel.PACKAGE)
    private boolean override, enabled;

    private final List<String> aliases;
    private final Map<String, String> subCommands;

    private final String description, usage;
    private final String permissionMessage;

    @Getter(AccessLevel.NONE)
    final boolean hasParent;
    final String parentName;

    CommandFile(String name, ConfigurationSection section, SIRCommand parent) {
        Objects.requireNonNull(section, "Configuration section cannot be null");

        this.name = Objects.requireNonNull(name, "Command name cannot be null");
        permission = section.getString("permission", "sir." + this.name);
        Objects.requireNonNull(permission, "Command permission cannot be null");

        aliases = Configurable.toStringList(section, "aliases");

        Map<String, String> subCommands = new LinkedHashMap<>();
        ConfigurationSection sub = section.getConfigurationSection("sub-commands");
        if (sub != null)
            sub.getKeys(false).forEach(key -> subCommands.put(key, sub.getString(key, permission + "." + key)));

        this.subCommands = new LinkedHashMap<>(subCommands);

        description = section.getString("description", "");
        usage = section.getString("usage", "/" + this.name);
        permissionMessage = section.getString("permission-message", "You do not have permission to execute this command.");

        this.hasParent = section.getBoolean("depends.enabled");
        parentName = section.getString("depends.parent");

        boolean enabled = section.getBoolean("enabled", true);
        boolean override = section.getBoolean("override-existing", false);

        SIRCommand resolved = hasParent() && parent != null &&
                !parent.getName().equalsIgnoreCase(parentName) ? parent : null;

        if (hasParent() && resolved != null) {
            enabled = resolved.isEnabled();
            override = resolved.isOverriding();
        }

        this.enabled = enabled;
        this.override = override;
    }

    boolean hasParent() {
        return hasParent && StringUtils.isNotBlank(parentName);
    }
}