package com.bitaspire.sir.module;

import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import com.google.common.base.Preconditions;
import lombok.Getter;
import me.croabeast.common.util.ArrayUtils;
import me.croabeast.file.Configurable;
import com.bitaspire.sir.Information;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Metadata descriptor for a {@link SIRModule}, parsed from its bundled {@code module.yml}.
 *
 * <p> Holds the module's identity (main class, name, title, description) and its
 * declared plugin dependencies.
 */
@Getter
public final class ModuleInformation implements Information {

    /** Fully-qualified main class name of the module. */
    @NotNull
    private final String main;

    /** Internal identifier name derived from the configuration or the main class. */
    @NotNull
    private final String name;

    /** Human-readable display title shown in the GUI. */
    @NotNull
    private final String title;

    /** Description lines shown in the GUI item lore. */
    private final String[] description;

    /** GUI slot position assigned during module loading. */
    private final Slot slot;

    /** Hard plugin dependencies - the module will not load if any are missing. */
    private final List<String> depend;

    /** Optional plugin dependencies - the module loads without them but may have reduced functionality. */
    private final List<String> softDepend;

    @ApiStatus.Internal
    public ModuleInformation(FileConfiguration configuration) {
        String main = configuration.getString("main");
        Preconditions.checkArgument(StringUtils.isNotBlank(main), "Module main class cannot be null or empty");
        this.main = main;

        String[] mainParts = main.split("\\.");
        this.name = configuration.getString("name", mainParts[mainParts.length - 1]);

        List<String> list = ArrayUtils.toList(name.replaceAll("[-_]", " ").split(" "));
        list.replaceAll(WordUtils::capitalize);
        this.title = configuration.getString("title", String.join(" ", list));

        this.description = Configurable.toStringList(configuration, "description").toArray(new String[0]);
        this.slot = Slot.fromXY(0, 0);

        this.depend = Collections.unmodifiableList(Configurable.toStringList(configuration, "depend"));

        List<String> soft = Configurable.toStringList(configuration, "soft-depend");
        this.softDepend = Collections.unmodifiableList(soft.stream().distinct().collect(Collectors.toList()));
    }
}
