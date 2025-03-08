package me.croabeast.sir.plugin.module;

import me.croabeast.lib.file.ResourceUtils;
import me.croabeast.lib.util.ArrayUtils;
import me.croabeast.sir.plugin.file.FileData;
import me.croabeast.sir.plugin.file.FileKey;
import me.croabeast.takion.logger.TakionLogger;
import me.croabeast.takion.misc.StringAligner;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;
import org.bukkit.util.Consumer;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.function.UnaryOperator;

final class MotdHandler extends ListenerModule {

    private final List<String[]> motdList = new ArrayList<>();
    private final List<CachedServerIcon> iconList = new ArrayList<>();

    private final FileKey<String> key;
    private int motdIndex = 0, iconIndex = 0;

    private final File folder;
    private CachedServerIcon defIcon;

    private final Random random = new Random();

    MotdHandler() {
        super(Key.MOTD);
        key = FileData.Module.MOTD;

        folder = plugin.fileFrom("modules", "motd", "icons");
        if (!folder.exists()) folder.mkdirs();

        File icon = ResourceUtils.fileFrom(folder, "server-icon.png");
        if (icon.exists()) return;

        String sep = File.separator;
        String path = icon.getPath();

        String iconPath = "resources" + sep + path.replace(sep, "/");

        try {
            ResourceUtils.saveResource(
                    plugin.getResource(iconPath),
                    plugin.getDataFolder(), path, false
            );
        } catch (Exception ignored) {}

        try {
            defIcon = Bukkit.loadServerIcon(icon);
        } catch (Exception ignored) {}
    }

    @Override
    public boolean register() {
        boolean atLeastOneFeatureWasLoaded = false;
        super.register();

        final ConfigurationSection motds = key.getFile("motds").getSection("motds");
        if (motds != null) {
            Set<String> keys = motds.getKeys(false);

            for (String key : keys) {
                ConfigurationSection s = motds.getConfigurationSection(key);
                if (s == null) continue;

                final String first = s.getString("1");
                if (first != null)
                    motdList.add(new String[] {first, s.getString("2")});
            }

            if (!motdList.isEmpty()) atLeastOneFeatureWasLoaded = true;
        }

        File[] icons = folder.listFiles((d, n) -> n.endsWith(".png"));
        if (!ArrayUtils.isArrayEmpty(icons)) {
            for (File file : icons)
                try {
                    iconList.add(Bukkit.loadServerIcon(file));
                } catch (Exception ignored) {}

            if (!iconList.isEmpty() && !atLeastOneFeatureWasLoaded)
                atLeastOneFeatureWasLoaded = true;
        }

        return atLeastOneFeatureWasLoaded;
    }

    enum MaxPlayers {
        MAXIMUM, CUSTOM, DEFAULT
    }

    enum IconInput {
        DISABLED, SINGLE, RANDOM, LIST
    }

    private MaxPlayers getMaxPlayers() {
        String input = key.getFile().get("max-players.type", "DEFAULT");

        try {
            return MaxPlayers.valueOf(input.toUpperCase(Locale.ENGLISH));
        } catch (Exception e) {
            return MaxPlayers.DEFAULT;
        }
    }

    private IconInput getIconInput() {
        String input = key.getFile().get("server-icon.usage", "DISABLED");

        try {
            return IconInput.valueOf(input.toUpperCase(Locale.ENGLISH));
        } catch (Exception e) {
            return IconInput.DISABLED;
        }
    }

    @EventHandler
    private void onServerPing(ServerListPingEvent event) {
        if (!this.isEnabled()) return;

        Player player = null;
        for (OfflinePlayer off : Bukkit.getOfflinePlayers()) {
            Player temp = off.getPlayer();
            if (temp == null) continue;

            InetSocketAddress address = temp.getAddress();
            if (address == null) continue;

            if (address.getAddress() == event.getAddress())
                player = temp;
        }

        int count = motdList.size() - 1;
        if (motdIndex > count) motdIndex = 0;

        final Player p = player;
        UnaryOperator<String> operator = s ->
                StringAligner.align(
                        134,
                        plugin.getLibrary().colorize(p, s)
                );

        String[] array = motdList.get(motdIndex);
        String motd = array[1] != null ?
                String.join("\n", array) : array[0];

        event.setMotd(operator.apply(motd));

        if (key.getFile().get("random-motds", false)) {
            motdIndex = random.nextInt(count + 1);
            return;
        }

        motdIndex = motdIndex < count ? (motdIndex + 1) : 0;

        ((Consumer<MaxPlayers>) maxInput -> {
            if (maxInput == MaxPlayers.DEFAULT) return;

            int custom = key.getFile().get("max-players.count", 0);

            event.setMaxPlayers(
                    maxInput == MaxPlayers.CUSTOM ?
                            custom :
                            Bukkit.getOnlinePlayers().size() + 1
            );
        }).accept(getMaxPlayers());

        ((Consumer<IconInput>) input -> {
            if (input == IconInput.DISABLED) return;

            boolean single = input == IconInput.SINGLE || iconList.isEmpty();

            int iconCount = iconList.size() - 1;
            if (!single && iconIndex > iconCount) iconIndex = 0;

            CachedServerIcon icon = single ? defIcon : iconList.get(iconIndex);
            try {
                event.setServerIcon(icon);
            } catch (Exception e) {
                TakionLogger.getLogger().log(
                        "&cError when trying to set the server icon.",
                        "&7Your server doesn't support this feature.",
                        "&cAvoid this error upgrading your server jar!"
                );
                e.printStackTrace();
            }

            if (single) return;

            switch (input) {
                case LIST:
                    iconIndex = iconIndex < iconCount ? (iconIndex + 1) : 0;
                    break;

                case RANDOM:
                    iconIndex = new Random().nextInt(iconCount + 1);
                    break;

                default: break;
            }
        }).accept(getIconInput());
    }
}
