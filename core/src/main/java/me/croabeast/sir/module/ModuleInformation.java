package me.croabeast.sir.module;

import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import com.google.common.base.Preconditions;
import lombok.Getter;
import me.croabeast.common.util.ArrayUtils;
import me.croabeast.file.Configurable;
import me.croabeast.sir.Information;
import me.croabeast.sir.Toggleable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public final class ModuleInformation implements Information {

    @NotNull
    private final String main, name, title;

    private final String[] description;
    private final Slot slot;

    private final List<String> depend, softDepend;

    ModuleInformation(FileConfiguration configuration) {
        String main = configuration.getString("main");
        Preconditions.checkArgument(StringUtils.isNotBlank(main), "Module main class cannot be null or empty");
        this.main = main;

        String[] mainParts = main.split("\\.");
        this.name = configuration.getString("name", mainParts[mainParts.length - 1]);

        List<String> list = ArrayUtils.toList(name.replaceAll("[-_]", " ").split(" "));
        list.replaceAll(WordUtils::capitalize);
        this.title = configuration.getString("title", String.join(" ", list));

        this.description = Configurable.toStringList(configuration, "description").toArray(new String[0]);
        this.slot = Slot.fromXY(
                configuration.getInt("slot-coordinates.x", 0),
                configuration.getInt("slot-coordinates.y", 0)
        );

        this.depend = Collections.unmodifiableList(Configurable.toStringList(configuration, "depend"));

        List<String> soft = Configurable.toStringList(configuration, "soft-depend");
        this.softDepend = Collections.unmodifiableList(soft.stream().distinct().collect(Collectors.toList()));
    }
}
