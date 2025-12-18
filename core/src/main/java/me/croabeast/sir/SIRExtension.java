package me.croabeast.sir;

import me.croabeast.common.Registrable;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.logging.Level;

public interface SIRExtension extends Registrable {

    boolean isEnabled();

    @NotNull
    String getName();

    @NotNull
    File getDataFolder();

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

    default void saveResource(@NotNull String path, boolean replace) {
        if (path.isEmpty()) throw new IllegalArgumentException("Path cannot be null or empty");

        path = path.replace('\\', '/');
        InputStream in = getResource(path);
        if (in == null)
            throw new IllegalArgumentException("The embedded resource '" + path + "' cannot be found");

        File outDir = new File(getDataFolder(), path.substring(0, Math.max(path.lastIndexOf('/'), 0)));
        if (!outDir.exists()) outDir.mkdirs();

        File outFile = new File(getDataFolder(), path);
        try {
            if (outFile.exists() && !replace) return;

            OutputStream out = Files.newOutputStream(outFile.toPath());
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);

            out.close();
            in.close();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not save " + outFile.getName() + " to " + outFile, e);
        }
    }
}
