package me.croabeast.sir.module.motd;

import lombok.Getter;
import me.croabeast.common.Loadable;
import me.croabeast.common.util.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.util.CachedServerIcon;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Getter
final class IconLoader implements Loadable {

    private final File iconFolder;
    private boolean loaded = false;

    private final List<CachedServerIcon> icons = new ArrayList<>();
    private CachedServerIcon defaultIcon;

    IconLoader(MOTD main) {
        iconFolder = new File(main.getDataFolder(), "icons");
        if (!iconFolder.exists()) iconFolder.mkdirs();

        File icon = new File(iconFolder, "server-icon.png");
        if (icon.exists() || !main.config.isAlwaysLoadDefaultIcon())
            return;

        String path = icon.getPath().replace(File.separatorChar, '/');
        try {
            main.saveResource(path, true);
        } catch (Exception ignored) {}

        try {
            defaultIcon = Bukkit.loadServerIcon(icon);
        } catch (Exception ignored) {}
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
