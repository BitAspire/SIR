package me.croabeast.sir.plugin.file;

import me.croabeast.lib.file.ConfigurableFile;
import me.croabeast.sir.api.BaseKey;
import org.jetbrains.annotations.NotNull;

public interface FileKey<O> extends BaseKey {

    @NotNull
    ConfigurableFile getFile();

    default ConfigurableFile getFile(O object) {
        throw new UnsupportedOperationException("Multiple files not supported here.");
    }
}
