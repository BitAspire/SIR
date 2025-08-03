package me.croabeast.sir.misc;

import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.BaseKey;
import org.jetbrains.annotations.NotNull;

public interface FileKey<T> extends BaseKey {

    @NotNull
    ConfigurableFile getFile();

    default ConfigurableFile getFile(T object) {
        throw new UnsupportedOperationException("Multiple files not supported here.");
    }
}
