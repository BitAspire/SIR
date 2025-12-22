package me.croabeast.sir.command;

import lombok.Getter;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.Toggleable;
import org.jetbrains.annotations.NotNull;

import java.io.File;

@Getter
public abstract class StandaloneProvider implements CommandProvider {

    protected boolean registered = true;

    private SIRApi api;
    private ProviderInformation information;

    private Toggleable.Button button;

    private File dataFolder;
    private ClassLoader classLoader;

    final void init(@NotNull SIRApi api, @NotNull ClassLoader loader, @NotNull ProviderInformation information) {
        this.api = api;
        this.information = information;
        this.classLoader = loader;

        button = new Toggleable.Button(information, this, true);
        button.setDefaultItems();
        button.setOnClick(b -> event -> {
            api.getLibrary().getLogger().log("Provider '" + getName() + "' loaded: " + b.isEnabled());
            b.toggleRegistering();
        });

        dataFolder = new File(api.getPlugin().getDataFolder(), "commands" + File.separator + getName());
    }

    @Override
    public final boolean isEnabled() {
        return button.isEnabled();
    }

    @NotNull
    public final String getName() {
        return information.getName();
    }
}
