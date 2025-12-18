package me.croabeast.sir;

import me.croabeast.file.ConfigurableFile;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class ExtensionFile extends ConfigurableFile {

    public ExtensionFile(SIRExtension loader, @Nullable String folder, String name, boolean initialize) throws IOException {
        super(loader, folder, name);
        if (initialize) saveDefaults();
    }

    public ExtensionFile(SIRExtension loader, String name, boolean initialize) throws IOException {
        super(loader, name);
        if (initialize) saveDefaults();
    }
}
