package com.bitaspire.sir.command;

import com.bitaspire.sir.SIRApi;
import lombok.Getter;
import com.bitaspire.sir.SIRExtension;
import com.bitaspire.sir.MenuToggleable;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * A {@link CommandProvider} loaded from an external jar and managed by the {@link CommandManager}.
 *
 * <p> Subclasses declare their commands and are initialized with {@link ProviderInformation}
 * parsed from the bundled {@code commands.yml}. The GUI toggle button (if supported) allows
 * server administrators to enable/disable the provider and configure command overrides at runtime.
 */
@Accessors(makeFinal = true)
@Getter
public abstract class StandaloneProvider extends SIRExtension<ProviderInformation> implements CommandProvider {

    { registered = true; }

    private ProviderInformation information;
    private File dataFolder;

    @Override
    protected final void onInit(SIRApi api, ClassLoader loader, ProviderInformation information) {
        this.information = information;
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

                api.getCommandManager().updateProviderEnabled(information.getName(), !b.isEnabled(), true);
            });
        }

        dataFolder = new File(api.getPlugin().getDataFolder(), "commands" + File.separator + getName());
    }

    @NotNull
    public final String getName() {
        return information.getName();
    }
}
