package com.bitaspire.sir.command;

import lombok.Getter;
import com.bitaspire.sir.SIRApi;
import com.bitaspire.sir.MenuToggleable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.File;

@Getter
public abstract class StandaloneProvider implements CommandProvider {

    protected boolean registered = true;

    private SIRApi api;
    private ProviderInformation information;

    private MenuToggleable.Button button;
    private boolean enabledState;

    private File dataFolder;
    private ClassLoader classLoader;

    @ApiStatus.Internal
    public final void init(@NotNull SIRApi api, @NotNull ClassLoader loader, @NotNull ProviderInformation information) {
        this.api = api;
        this.information = information;
        this.classLoader = loader;
        this.enabledState = api.getCommandManager().isProviderEnabled(information.getName());

        if (MenuToggleable.supportsButtons()) {
            button = new MenuToggleable.Button(information, this, enabledState);
            button.setDefaultItems();
            button.allowToggle(false);

            button.setOnClick(b -> event -> {
                if (event.isRightClick()) {
                    api.getCommandManager().openOverrideMenu(this, event, true);
                    return;
                }

                if (!event.isLeftClick()) return;

                b.toggle();
                enabledState = b.isEnabled();
                api.getLibrary().getLogger().log("Provider '" + getName() + "' loaded: " + b.isEnabled());
                b.toggleRegistering();
                api.getCommandManager().setProviderEnabled(information.getName(), b.isEnabled());
            });
        }

        dataFolder = new File(api.getPlugin().getDataFolder(), "commands" + File.separator + getName());
    }

    @Override
    public final boolean isEnabled() {
        return button != null ? button.isEnabled() : enabledState;
    }

    @ApiStatus.Internal
    public final void setEnabledState(boolean enabled) {
        this.enabledState = enabled;
    }

    @ApiStatus.Internal
    public final void setRegistered(boolean registered) {
        this.registered = registered;
    }

    @NotNull
    public final String getName() {
        return information.getName();
    }
}
