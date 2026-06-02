package com.bitaspire.sir.module;

import com.bitaspire.sir.SIRApi;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.croabeast.common.util.ArrayUtils;
import com.bitaspire.sir.Information;
import com.bitaspire.sir.SIRExtension;
import com.bitaspire.sir.MenuToggleable;
import com.bitaspire.sir.command.CommandProvider;
import com.bitaspire.sir.command.SIRCommand;
import me.croabeast.takion.logger.TakionLogger;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base class for all SIR modules loaded from external jars.
 *
 * <p> Subclasses are discovered and instantiated by the {@link ModuleManager}. A module
 * is initialized with its {@link ModuleInformation} and optionally exposes a GUI toggle
 * button to enable/disable it at runtime. Modules that also implement {@link com.bitaspire.sir.command.CommandProvider}
 * have their commands reflected in the button description automatically.
 */
@Accessors(makeFinal = true)
@Getter
public abstract class SIRModule extends SIRExtension<ModuleInformation> implements MenuToggleable {

    private ModuleInformation information;
    private File dataFolder;

    @Override
    protected final void onInit(SIRApi api, ClassLoader loader, ModuleInformation information) {
        this.information = information;
        this.enabledState = api.getModuleManager().isEnabled(information.getName());

        if (MenuToggleable.supportsButtons()) {
            button = new Button(buildInformation(), this, enabledState);
            button.setDefaultItems();
            button.allowToggle(false);

            button.setOnClick(b -> event -> {
                if (event.isRightClick()) {
                    api.getModuleManager().openConfigMenu(event);
                    return;
                }

                if (!event.isLeftClick()) return;

                boolean enabled = !b.isEnabled();
                api.getModuleManager().updateEnabled(this, enabled);
                if (isEnabled() == enabled)
                    api.getLibrary().getLogger().log("Module '" + getName() + "' loaded: " + enabled);
            });
        }

        dataFolder = new File(api.getPlugin().getDataFolder(), "modules" + File.separator + getName());
        if (!dataFolder.exists()) dataFolder.mkdirs();
    }

    private Information buildInformation() {
        if (!(this instanceof CommandProvider)) return information;

        Set<SIRCommand> commands = ((CommandProvider) this).getCommands();
        if (commands.isEmpty()) return information;

        List<String> commandNames = commands.stream()
                .filter(Objects::nonNull)
                .filter(SIRCommand::isEnabled)
                .map(SIRCommand::getName)
                .filter(StringUtils::isNotBlank)
                .map(name -> "/" + name)
                .distinct()
                .collect(Collectors.toList());

        if (commandNames.isEmpty()) return information;

        List<String> description = new ArrayList<>(ArrayUtils.toList(information.getDescription()));
        description.add("");
        description.add("Commands: " + String.join(", ", commandNames));

        return new Information() {
            @NotNull
            public String getName() {
                return information.getName();
            }

            @NotNull
            public String getTitle() {
                return information.getTitle();
            }

            @NotNull
            public String[] getDescription() {
                return description.toArray(new String[0]);
            }

            @NotNull
            public Slot getSlot() {
                return information.getSlot();
            }
        };
    }

    @NotNull
    @Override
    public final File getDataFolder() {
        if (dataFolder == null && api != null)
            dataFolder = new File(api.getPlugin().getDataFolder(), "modules" + File.separator + getName());

        if (dataFolder != null && !dataFolder.exists())
            dataFolder.mkdirs();

        return Objects.requireNonNull(dataFolder);
    }

    @NotNull
    @Override
    public final String getName() {
        return information.getName();
    }

    @NotNull
    public final TakionLogger getLogger() {
        return api.getLibrary().getLogger();
    }
}
