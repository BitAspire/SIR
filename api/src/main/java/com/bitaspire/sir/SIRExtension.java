package com.bitaspire.sir;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.croabeast.common.Registrable;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;

/**
 * Base class for all SIR extensions (modules, addons, command providers).
 *
 * <p> Handles lifecycle initialization, resource access, and toggle state. Subclasses
 * must implement {@link #onInit(SIRApi, ClassLoader, Object)} to perform their own setup,
 * and {@link #getName()} / {@link #getDataFolder()} to declare their identity.
 *
 * @param <I> the information type describing this extension (e.g., {@link com.bitaspire.sir.module.ModuleInformation}).
 */
@Accessors(makeFinal = true)
public abstract class SIRExtension<I> implements Registrable {

    /** The active {@link SIRApi} instance, set during initialization. */
    @Getter
    protected SIRApi api;

    /** The GUI toggle button for this extension, or {@code null} if buttons are not supported. */
    @Getter
    protected MenuToggleable.Button button;

    /** The raw enabled state used when no {@link MenuToggleable.Button} is present. */
    @Setter(AccessLevel.PACKAGE)
    protected boolean enabledState;

    /** Whether this extension is currently registered with the server. */
    @Setter(AccessLevel.PACKAGE)
    @Getter
    protected boolean registered;

    /** The class loader used to load resources bundled with this extension. */
    @Getter
    protected ClassLoader classLoader;

    /**
     * Returns whether this extension is currently enabled.
     *
     * <p> Delegates to the toggle button if present, otherwise uses {@link #enabledState}.
     *
     * @return {@code true} if enabled.
     */
    public final boolean isEnabled() {
        return button != null ? button.isEnabled() : enabledState;
    }

    /**
     * Called internally to initialize this extension.
     *
     * @param api the API instance.
     * @param loader the class loader for this extension.
     * @param information the parsed information descriptor.
     */
    final void init(SIRApi api, ClassLoader loader, I information) {
        this.api = api;
        this.classLoader = loader;
        onInit(api, loader, information);
    }

    /**
     * Extension-specific initialization logic called after the base fields are set.
     *
     * @param api the API instance.
     * @param loader the class loader for this extension.
     * @param information the parsed information descriptor.
     */
    protected abstract void onInit(SIRApi api, ClassLoader loader, I information);

    /**
     * Returns the unique internal name of this extension.
     *
     * @return the name.
     */
    @NotNull
    public abstract String getName();

    /**
     * Returns the data folder where this extension stores its files.
     *
     * @return the data folder.
     */
    @NotNull
    public abstract File getDataFolder();

    /**
     * Returns an {@link InputStream} for a resource bundled in this extension's jar.
     *
     * @param path the resource path relative to the jar root.
     * @return the input stream, or {@code null} if the resource does not exist or the path is blank.
     */
    @Nullable
    public InputStream getResource(String path) {
        if (StringUtils.isBlank(path)) return null;

        try {
            URL url = getClassLoader().getResource(path);
            if (url == null) return null;

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);

            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Saves a bundled resource to the extension's data folder.
     *
     * @param path the resource path relative to the jar root.
     * @param replace if {@code true}, overwrites an existing file.
     * @throws IllegalArgumentException if the path is empty or the resource cannot be found or written.
     */
    public void saveResource(@NotNull String path, boolean replace) {
        if (path.isEmpty()) throw new IllegalArgumentException("Path cannot be null or empty");

        path = path.replace('\\', '/');
        File outDir = new File(getDataFolder(), path.substring(0, Math.max(path.lastIndexOf('/'), 0)));
        if (!outDir.exists()) outDir.mkdirs();

        File outFile = new File(getDataFolder(), path);
        try (InputStream in = getResource(path)) {
            if (in == null)
                throw new IllegalArgumentException("The embedded resource '" + path + "' cannot be found");

            if (outFile.exists() && !replace) return;

            try (OutputStream out = Files.newOutputStream(outFile.toPath())) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not save " + outFile.getName() + " to " + outFile, e);
        }
    }
}
