package me.croabeast.sir.module;

import me.croabeast.file.ConfigurableFile;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class ModuleFile extends ConfigurableFile {

    public ModuleFile(SIRModule loader, @Nullable String folder, String name) throws IOException {
        super(loader, folder, name);
    }

    public ModuleFile(SIRModule loader, String name) throws IOException {
        super(loader, name);
    }
}
