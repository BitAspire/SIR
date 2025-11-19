package me.croabeast.sir;

import me.croabeast.common.Loadable;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public interface SIRExtension extends Loadable {

    boolean isEnabled();

    @NotNull
    String getName();

    @NotNull
    ClassLoader getClassLoader();

    @Nullable
    default InputStream getResource(String path) {
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
}
