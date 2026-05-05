package com.bitaspire.sir.addon;

import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.croabeast.common.util.ArrayUtils;
import com.bitaspire.sir.Information;
import com.bitaspire.sir.MenuToggleable;
import com.bitaspire.sir.SIRApi;
import com.bitaspire.sir.SIRExtension;
import com.bitaspire.sir.command.CommandProvider;
import com.bitaspire.sir.command.SIRCommand;
import me.croabeast.takion.logger.TakionLogger;
import org.jetbrains.annotations.ApiStatus;
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
public abstract class SIRAddon implements SIRExtension, MenuToggleable {

    private SIRApi api;
    private AddonInformation information;

    private Button button;
    private boolean enabledState;

    private File dataFolder;
    private ClassLoader classLoader;

    @ApiStatus.Internal
    public final void init(@NotNull SIRApi api, @NotNull URLClassLoader loader, @NotNull AddonInformation information) {
        this.api = api;
        this.information = information;
        this.enabledState = api.getAddonManager().isEnabled(information.getName());

        if (MenuToggleable.supportsButtons()) {
            button = new Button(buildInformation(), this, enabledState);
            button.setDefaultItems();
            button.allowToggle(false);

            button.setOnClick(b -> event -> {
                if (!event.isLeftClick()) return;

                b.toggle();
                enabledState = b.isEnabled();
                api.getLibrary().getLogger().log("Addon '" + getName() + "' loaded: " + b.isEnabled());
                b.toggleRegistering();
                api.getAddonManager().setEnabled(getName(), b.isEnabled());
            });
        }

        this.classLoader = loader;

        dataFolder = new File(api.getPlugin().getDataFolder(), "addons" + File.separator + getName());
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
    public final File getDataFolder() {
        if (dataFolder == null && api != null)
            dataFolder = new File(api.getPlugin().getDataFolder(), "addons" + File.separator + getName());

        if (dataFolder != null && !dataFolder.exists())
            dataFolder.mkdirs();

        return Objects.requireNonNull(dataFolder);
    }

    private boolean registered = false;

    @ApiStatus.Internal
    public final void setRegistered(boolean registered) {
        this.registered = registered;
    }

    @Override
    public final boolean isEnabled() {
        return button != null ? button.isEnabled() : enabledState;
    }

    @ApiStatus.Internal
    public final void setEnabledState(boolean enabled) {
        this.enabledState = enabled;
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
