package me.croabeast.sir.module;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.croabeast.common.Registrable;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.SIRExtension;
import me.croabeast.sir.Toggleable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URLClassLoader;

@Accessors(makeFinal = true)
@Getter
public abstract class SIRModule implements SIRExtension, Toggleable {

    private SIRApi api;
    private ModuleInformation information;

    private Button button;

    private File dataFolder;
    private ClassLoader classLoader;

    final void init(@NotNull SIRApi api, @NotNull URLClassLoader loader, @NotNull ModuleInformation file) {
        this.api = api;
        this.information = file;

        button = new Button(information, new Registrable() {
            @Override
            public boolean isRegistered() {
                return isLoaded();
            }

            @Override
            public boolean register() {
                load();
                return true;
            }

            @Override
            public boolean unregister() {
                unload();
                return true;
            }
        }, true);
        button.setDefaultItems();

        button.setOnClick(b -> event -> {
            api.getLibrary().getLogger().log("Module '" + getName() + "' loaded: " + b.isEnabled());
            b.toggleRegistering();
        });

        this.classLoader = loader;
        dataFolder = new File(api.getPlugin().getDataFolder(), "modules" + File.separator + getName());
    }

    @Setter(AccessLevel.PACKAGE)
    private boolean loaded = false;

    @Override
    public final boolean isEnabled() {
        return button.isEnabled();
    }

    @NotNull
    public final String getName() {
        return information.getName();
    }
}
