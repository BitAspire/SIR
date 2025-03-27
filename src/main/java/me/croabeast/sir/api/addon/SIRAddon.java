package me.croabeast.sir.api.addon;

import lombok.Getter;
import lombok.experimental.Accessors;
import me.croabeast.lib.file.ResourceUtils;
import me.croabeast.sir.api.SIRExtension;
import me.croabeast.sir.plugin.SIRPlugin;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

@Accessors(makeFinal = true)
@Getter
public abstract class SIRAddon implements SIRExtension {

    private ClassLoader classLoader;
    SIRPlugin plugin;

    private File file;
    private File dataFolder;

    private AddonFile descriptionFile;

    boolean loaded = false;
    boolean enabled = false;

    public SIRAddon() {
        ClassLoader loader = getClass().getClassLoader();
        if (!(loader instanceof AddonClassLoader))
            throw new IllegalStateException("This addon requires " + AddonClassLoader.class.getName());

        ((AddonClassLoader) loader).initialize(this);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    public final String getName() {
        return descriptionFile.getName();
    }

    @NotNull
    public final String getFullName() {
        return getName() + ' ' + descriptionFile.getVersion();
    }

    protected abstract boolean enable();

    protected abstract boolean disable();

    @Nullable
    public final InputStream getResource(String name) {
        if (StringUtils.isBlank(name)) return null;

        try {
            URL url = getClassLoader().getResource(name);
            if (url == null) return null;

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);

            return connection.getInputStream();
        }
        catch (IOException ex) {
            return null;
        }
    }

    public final void saveResource(String path, boolean replace) {
        if (StringUtils.isBlank(path)) return;

        try {
            path = path.replace('\\', '/');
            ResourceUtils.saveResource(getResource(path), getDataFolder(), path, replace);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final void saveResource(String path) {
        saveResource(path, false);
    }

    final void initialize(AddonClassLoader loader, File file, File folder, AddonFile description) {
        this.classLoader = loader;
        this.file = file;
        this.descriptionFile = description;
        this.dataFolder = folder;
    }
}
