package com.bitaspire.sir.file;

import com.bitaspire.sir.SIRExtension;
import me.croabeast.file.ConfigurableFile;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * A {@code ConfigurableFile} scoped to a {@link SIRExtension}'s data folder.
 *
 * <p> Optionally copies bundled defaults from the extension's jar on construction.
 */
public class ExtensionFile extends ConfigurableFile {

    /**
     * Creates a new extension file inside an optional sub-folder, optionally saving defaults.
     *
     * @param loader the extension that owns this file.
     * @param folder an optional sub-folder name within the extension's data folder, or {@code null}.
     * @param name the file name (with extension).
     * @param initialize if {@code true}, copies the bundled default resource if the file does not exist.
     * @throws IOException if the file cannot be created or read.
     */
    public ExtensionFile(SIRExtension<?> loader, @Nullable String folder, String name, boolean initialize) throws IOException {
        super(loader, folder, name);
        if (initialize) saveDefaults();
    }

    /**
     * Creates a new extension file directly in the extension's data folder, optionally saving defaults.
     *
     * @param loader the extension that owns this file.
     * @param name the file name (with extension).
     * @param initialize if {@code true}, copies the bundled default resource if the file does not exist.
     * @throws IOException if the file cannot be created or read.
     */
    public ExtensionFile(SIRExtension<?> loader, String name, boolean initialize) throws IOException {
        super(loader, name);
        if (initialize) saveDefaults();
    }
}
