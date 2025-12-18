package me.croabeast.sir.module.tag;

import lombok.Getter;
import me.croabeast.file.Configurable;
import me.croabeast.sir.PermissibleUnit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

@Getter
final class Tag implements PermissibleUnit {

    private final ConfigurationSection section;

    private final String tag;
    private final List<String> description;

    Tag(ConfigurationSection section) {
        this.section = section;
        tag = section.getString("tag");
        description = Configurable.toStringList(section, "description");
    }
}
