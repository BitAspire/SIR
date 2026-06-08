package com.bitaspire.sir.module.motd;

import lombok.Getter;
import me.croabeast.common.Loadable;
import me.croabeast.common.util.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.util.CachedServerIcon;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Getter
final class IconLoader implements Loadable {

    private static final String ICON_FOLDER_NAME = "icons";
    private static final String DEFAULT_ICON_NAME = "server-icon.png";
    private static final String DEFAULT_ICON_RESOURCE = ICON_FOLDER_NAME + "/" + DEFAULT_ICON_NAME;

    private final File iconFolder;
    private boolean loaded = false;

    private final List<CachedServerIcon> icons = new ArrayList<>();
    private CachedServerIcon defaultIcon;

    IconLoader(MOTD main) {
        iconFolder = new File(main.getDataFolder(), ICON_FOLDER_NAME);
        if (!iconFolder.exists()) iconFolder.mkdirs();
        cleanupLegacyNestedFolder(main);

        String iconName = normalizeIconName(main.config.getServerIconImage());

        File icon = new File(iconFolder, iconName);
        if (!icon.exists() && main.config.isAlwaysLoadDefaultIcon() && DEFAULT_ICON_NAME.equals(iconName))
            saveDefaultIcon(main, icon);

        try {
            defaultIcon = Bukkit.loadServerIcon(icon);
        } catch (Exception ignored) {}
    }

    private String normalizeIconName(String iconName) {
        if (iconName == null) return DEFAULT_ICON_NAME;

        iconName = iconName.trim().replace('\\', '/');
        if (iconName.isEmpty()) return DEFAULT_ICON_NAME;

        int index = iconName.lastIndexOf('/');
        if (index >= 0) iconName = iconName.substring(index + 1);

        return iconName.isEmpty() ? DEFAULT_ICON_NAME : iconName;
    }

    private void saveDefaultIcon(MOTD main, File icon) {
        File parent = icon.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        try (InputStream input = main.getResource(DEFAULT_ICON_RESOURCE)) {
            if (input != null && !icon.exists())
                Files.copy(input, icon.toPath());
        } catch (Exception ignored) {}
    }

    private void cleanupLegacyNestedFolder(MOTD main) {
        File dataFolder = main.getDataFolder();
        File legacyRoot = new File(dataFolder, dataFolder.getPath());

        try {
            File dataCanonical = dataFolder.getCanonicalFile();
            File rootCanonical = legacyRoot.getCanonicalFile();
            if (!isChild(dataCanonical, rootCanonical)) return;

            File legacyIcon = new File(rootCanonical, ICON_FOLDER_NAME + File.separator + DEFAULT_ICON_NAME);
            if (legacyIcon.isFile()) legacyIcon.delete();

            deleteEmptyParents(legacyIcon.getParentFile(), dataCanonical);
        } catch (Exception ignored) {}
    }

    private boolean isChild(File parent, File child) {
        String parentPath = parent.getPath();
        String childPath = child.getPath();
        return !parentPath.equals(childPath) && childPath.startsWith(parentPath + File.separator);
    }

    private void deleteEmptyParents(File file, File stop) {
        while (file != null && !file.equals(stop)) {
            File[] children = file.listFiles();
            if (children == null || children.length > 0 || !file.delete()) return;

            file = file.getParentFile();
        }
    }

    @Override
    public void load() {
        if (isLoaded()) return;

        File[] icons = iconFolder.listFiles((d, n) -> n.endsWith(".png"));
        if (ArrayUtils.isArrayEmpty(icons)) return;

        for (File icon : icons)
            try {
                this.icons.add(Bukkit.loadServerIcon(icon));
            } catch (Exception ignored) {}

        loaded = !this.icons.isEmpty();
    }

    @Override
    public void unload() {
        if (!isLoaded()) return;

        icons.clear();
        loaded = false;
    }
}
