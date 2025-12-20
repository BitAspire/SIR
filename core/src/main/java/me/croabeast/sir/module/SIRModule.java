package me.croabeast.sir.module;

import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.croabeast.common.util.ArrayUtils;
import me.croabeast.sir.Information;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.SIRExtension;
import me.croabeast.sir.Toggleable;
import me.croabeast.sir.command.CommandProvider;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.takion.logger.TakionLogger;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Accessors(makeFinal = true)
@Getter
public abstract class SIRModule implements SIRExtension, Toggleable {

    private SIRApi api;
    private ModuleInformation information;

    private Button button;

    private File dataFolder;
    private ClassLoader classLoader;

    final void init(@NotNull SIRApi api, @NotNull URLClassLoader loader, @NotNull ModuleInformation information) {
        this.api = api;
        this.information = information;

        button = new Button(buildInformation(), this, true);
        button.setDefaultItems();

        button.setOnClick(b -> event -> {
            api.getLibrary().getLogger().log("Module '" + getName() + "' loaded: " + b.isEnabled());
            b.toggleRegistering();
        });

        this.classLoader = loader;
        dataFolder = new File(api.getPlugin().getDataFolder(), "modules" + File.separator + getName());
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

    @Setter(AccessLevel.PACKAGE)
    private boolean registered = false;

    @Override
    public final boolean isEnabled() {
        return button.isEnabled();
    }

    @NotNull
    public final String getName() {
        return information.getName();
    }

    @NotNull
    public final TakionLogger getLogger() {
        return api.getLibrary().getLogger();
    }
}
