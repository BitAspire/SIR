package me.croabeast.sir;

import com.Zrips.CMI.Containers.CMIUser;
import com.earth2me.essentials.Essentials;
import lombok.Getter;
import lombok.Setter;
import me.croabeast.common.Registrable;
import me.croabeast.common.util.Exceptions;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.scheduler.GlobalTask;
import me.croabeast.sir.user.*;
import me.leoko.advancedban.manager.PunishmentManager;
import me.leoko.advancedban.manager.UUIDManager;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class UserManagerImpl implements UserManager, Registrable {

    private final Map<UUID, BaseUser> userMap = new HashMap<>();

    private final SIRPlugin plugin;
    private final Listener listener;

    private final ConfigurableFile ignoreFile;
    private final ConfigurableFile muteFile;
    private final ConfigurableFile channelFile;
    private final ConfigurableFile colorFile;

    public UserManagerImpl(SIRPlugin plugin) {
        this.plugin = plugin;

        ignoreFile = createFile("ignore");
        muteFile = createFile("mute");
        channelFile = createFile("chat-view");
        colorFile = createFile("chat-color");

        listener = new Listener() {
            @EventHandler(priority = EventPriority.LOW)
            private void onJoin(PlayerJoinEvent event) {
                loadData(event.getPlayer());
            }

            @EventHandler(priority = EventPriority.HIGHEST)
            private void onQuit(PlayerQuitEvent event) {
                unloadUser(event.getPlayer());
            }
        };
    }

    private ConfigurableFile createFile(String name) {
        try {
            ensureUserFile(name);
            return new ConfigurableFile(plugin, "users", name);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void ensureUserFile(String name) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            return;
        }

        File usersFolder = new File(dataFolder, "users");
        if (!usersFolder.exists() && !usersFolder.mkdirs()) {
            return;
        }

        File userFile = new File(usersFolder, name + ".yml");
        if (userFile.exists()) {
            return;
        }

        try {
            userFile.createNewFile();
        } catch (Exception ignored) {
        }
    }

    void shutdown() {
        unregister();
        saveAllData();
        userMap.clear();
    }

    @Override
    public boolean isRegistered() {
        return listener.isRegistered();
    }

    @Override
    public boolean register() {
        boolean registered = listener.register();
        if (registered) {
            for (OfflinePlayer offline : plugin.getServer().getOfflinePlayers()) {
                loadData(offline);
            }
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                loadData(player);
            }
        }

        return registered;
    }

    @Override
    public boolean unregister() {
        return listener.unregister();
    }

    private void loadData(Player player) {
        if (player == null) return;

        UUID uuid = player.getUniqueId();

        BaseUser onlineUser = new OnlineUser(player);
        userMap.put(uuid, onlineUser);
        onlineUser.load();
    }

    private void loadData(OfflinePlayer offline) {
        if (offline == null) return;

        UUID uuid = offline.getUniqueId();
        if (userMap.containsKey(uuid)) return;

        BaseUser offlineUser = new OfflineUser(offline);
        userMap.put(uuid, offlineUser);
        offlineUser.load();
    }

    private void unloadUser(Player player) {
        if (player == null) return;

        UUID uuid = player.getUniqueId();
        BaseUser user = userMap.get(uuid);
        if (user == null) return;

        user.getImmuneData().giveImmunity(0);
        user.save(true);

        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        BaseUser offlineUser = new OfflineUser(offline);
        userMap.put(uuid, offlineUser);
        offlineUser.load();
    }

    private void saveAllData() {
        for (BaseUser user : new ArrayList<>(userMap.values())) {
            if (user == null) continue;
            user.getImmuneData().giveImmunity(0);
            user.save(false);
        }

        saveFile(ignoreFile);
        saveFile(muteFile);
        saveFile(channelFile);
        saveFile(colorFile);
    }

    private void saveFile(ConfigurableFile file) {
        if (file != null) file.save();
    }

    @Override
    public SIRUser getUser(UUID uuid) {
        if (uuid == null) return null;

        SIRUser user = userMap.get(uuid);
        return user != null && user.isOnline() ? user : null;
    }

    @Override
    public SIRUser getUser(String name) {
        if (StringUtils.isBlank(name)) return null;

        for (SIRUser user : userMap.values()) {
            if (user == null || !user.isOnline()) continue;

            String userName = user.getName();
            if (userName.equals(name)) return user;
        }

        return null;
    }

    @Override
    public SIRUser fromClosest(String input) {
        if (StringUtils.isBlank(input)) return null;

        for (SIRUser user : userMap.values()) {
            if (user == null || !user.isOnline()) continue;

            String userName = user.getName();
            if (userName.matches("(?i)" + Pattern.quote(input)))
                return user;
        }

        return null;
    }

    @NotNull
    public Set<SIRUser> getUsers(boolean online) {
        Set<SIRUser> users = userMap.values().stream()
                .filter(Objects::nonNull)
                .filter(user -> !online || user.isOnline())
                .collect(Collectors.toSet());
        return Collections.unmodifiableSet(users);
    }

    @Override
    public boolean hasPermission(CommandSender sender, String permission) {
        if (sender == null) return false;
        if (StringUtils.isBlank(permission)) return true;

        if (sender.isOp() && !plugin.getConfiguration().isOverrideOp())
            return true;

        return sender.hasPermission(permission);
    }

    abstract static class BaseData {

        final ConfigurableFile file;
        final String uuid;

        BaseData(ConfigurableFile file, String uuid) {
            this.file = file;
            this.uuid = uuid;
        }
    }

    final class IgnoreImpl extends BaseData implements IgnoreData {

        class UuidSet {

            final Set<UUID> set = new HashSet<>();

            void setAll(Collection<String> collection) {
                if (collection == null) return;

                set.clear();
                for (String value : collection) {
                    try {
                        set.add(UUID.fromString(value));
                    } catch (Exception ignored) {}
                }
            }

            List<String> asStrings() {
                return set.stream().map(UUID::toString).collect(Collectors.toList());
            }
        }

        final UuidSet chatSet = new UuidSet();
        final UuidSet msgSet = new UuidSet();
        boolean chatAll = false;
        boolean msgAll = false;

        IgnoreImpl(UUID uuid) {
            super(ignoreFile, uuid.toString());
        }

        @Override
        public boolean isIgnoring(SIRUser player, boolean chat) {
            return player != null && (chat ? chatSet : msgSet).set.contains(player.getUuid());
        }

        @Override
        public boolean isIgnoring(Player player, boolean chat) {
            return isIgnoring(getUser(player), chat);
        }

        @Override
        public boolean isIgnoringAll(boolean chat) {
            return chat ? chatAll : msgAll;
        }

        @Override
        public void ignore(SIRUser user, boolean chat) {
            if (user != null)
                (chat ? chatSet : msgSet).set.add(user.getUuid());
        }

        @Override
        public void ignore(Player player, boolean chat) {
            if (player != null)
                ignore(getUser(player), chat);
        }

        @Override
        public void ignoreAll(boolean chat) {
            if (chat) {
                chatAll = true;
                return;
            }

            msgAll = true;
        }

        @Override
        public void unignore(SIRUser user, boolean chat) {
            if (user != null)
                (chat ? chatSet : msgSet).set.remove(user.getUuid());
        }

        @Override
        public void unignore(Player player, boolean chat) {
            if (player != null)
                unignore(getUser(player), chat);
        }

        @Override
        public void unignoreAll(boolean chat) {
            if (chat) {
                chatAll = false;
                return;
            }

            msgAll = false;
        }

        void load() {
            if (file == null) return;

            chatSet.setAll(file.toStringList(uuid + ".chat.single"));
            msgSet.setAll(file.toStringList(uuid + ".msg.single"));
            chatAll = file.get(uuid + ".chat.all", false);
            msgAll = file.get(uuid + ".msg.all", false);
        }

        void save(boolean save) {
            if (file == null) return;

            file.set(uuid + ".chat.single", chatSet.asStrings());
            file.set(uuid + ".msg.single", msgSet.asStrings());
            file.set(uuid + ".chat.all", chatAll);
            file.set(uuid + ".msg.all", msgAll);

            if (save) file.save();
        }
    }

    final class MuteImpl extends BaseData implements MuteData {

        private final SIRUser user;

        private GlobalTask task = null;
        private long remaining = 0;
        private long expiresAt = 0;

        private boolean muted = false;
        @Getter
        private String muteBy;
        @Getter
        private String reason;

        MuteImpl(SIRUser user) {
            super(muteFile, user.getUuid().toString());
            this.user = user;

            muteBy = file == null ? "Unknown" : file.get("default.admin", "Unknown");
            reason = file == null ? "Not following server rules."
                    : file.get("default.mute-reason", "Not following server rules.");
        }

        @Override
        public boolean isMuted() {
            if (plugin.getConfiguration().isCheckMute()
                    && plugin.getCommandManager().isEnabled("mute"))
                return muted;

            Player player = user.getPlayer();
            if (player == null) return muted;

            if (Exceptions.isPluginEnabled("Essentials")) {
                Essentials essentials = JavaPlugin.getPlugin(Essentials.class);
                return essentials.getUser(player).isMuted();
            }

            if (Exceptions.isPluginEnabled("AdvancedBan")) {
                final String id = UUIDManager.get().getUUID(user.getName());
                return PunishmentManager.get().isMuted(id);
            }

            return Exceptions.isPluginEnabled("CMI") &&
                    CMIUser.getUser(player).isMuted();
        }

        @Override
        public void mute(long time, String reason, String by) {
            if (time == 0) {
                unmute();
                return;
            }

            if (StringUtils.isNotBlank(by) && !by.equals(muteBy))
                this.muteBy = by;

            if (StringUtils.isNotBlank(reason) && !reason.equals(this.reason))
                this.reason = reason;

            if (time > 0) {
                muted = true;
                remaining = time * 1000L;

                if (expiresAt < 1)
                    expiresAt = System.currentTimeMillis() + remaining;

                scheduleUnmute(remaining);
                return;
            }

            muted = true;
            remaining = -1;
            expiresAt = -1;
        }

        @Override
        public void mute(long time) {
            mute(time, reason, muteBy);
        }

        @Override
        public void unmute() {
            if (task != null) {
                task.cancel();
                task = null;
            }

            muted = false;
            remaining = 0;
            expiresAt = 0;
        }

        @Override
        public long getRemaining() {
            if (expiresAt < 0) return -1;
            return (expiresAt - System.currentTimeMillis()) / 1000;
        }

        void scheduleUnmute(long remaining) {
            if (task != null) {
                task.cancel();
                task = null;
            }

            task = plugin.getScheduler().runTaskLater(() -> {
                this.remaining = 0;
                muted = false;
                expiresAt = -1;
            }, remaining / 50L);
        }

        void load() {
            if (file == null) return;

            Object expires = file.get(uuid + ".expiresAt", -1L);
            Object remainingRaw = file.get(uuid + ".remaining", -1L);

            expiresAt = toLong(expires);
            remaining = toLong(remainingRaw);

            muteBy = file.get(uuid + ".admin", muteBy);
            reason = file.get(uuid + ".reason", reason);

            muted = file.get(uuid + ".muted", false);
            if (!muted) return;

            if (expiresAt == -1) {
                remaining = -1;
            } else if (System.currentTimeMillis() > expiresAt) {
                muted = false;
                remaining = 0;
            } else {
                remaining = expiresAt - System.currentTimeMillis();
                scheduleUnmute(remaining);
            }
        }

        private long toLong(Object value) {
            if (value instanceof Number) return ((Number) value).longValue();

            try {
                return Long.parseLong(String.valueOf(value));
            } catch (Exception ignored) {
                return -1L;
            }
        }

        void save(boolean save) {
            if (file == null) return;

            if (task != null) {
                task.cancel();
                task = null;
            }

            if (!muted) {
                file.set(uuid, null);
            } else {
                file.set(uuid + ".muted", true);
                file.set(uuid + ".remaining",
                        (expiresAt > 0) ? expiresAt - System.currentTimeMillis() : -1);

                file.set(uuid + ".expiresAt", expiresAt);
                file.set(uuid + ".admin", muteBy);
                file.set(uuid + ".reason", reason);
            }

            if (save) file.save();
        }
    }

    final class ChannelImpl extends BaseData implements ChannelData {

        private final Set<String> toggledChannels = new HashSet<>();

        ChannelImpl(UUID uuid) {
            super(channelFile, uuid.toString());
        }

        void load() {
            if (file == null) return;

            toggledChannels.clear();
            toggledChannels.addAll(file.toStringList(uuid));
        }

        void save(boolean save) {
            if (file == null) return;

            file.set(uuid, new ArrayList<>(toggledChannels));
            if (save) file.save();
        }

        @Override
        public void toggle(String channel) {
            if (StringUtils.isBlank(channel)) return;

            if (toggledChannels.contains(channel)) {
                toggledChannels.remove(channel);
                return;
            }

            toggledChannels.add(channel);
        }

        @Override
        public boolean isToggled(String channel) {
            return toggledChannels.contains(channel);
        }
    }

    final class ImmuneImpl extends BaseData implements ImmuneData {

        private final SIRUser user;

        @Getter
        private boolean immune = false;
        private GlobalTask task = null;

        ImmuneImpl(SIRUser user) {
            super(null, user.getUuid().toString());
            this.user = user;
        }

        private void cancelTask() {
            if (task != null) task.cancel();
        }

        void setImmune(boolean immune) {
            Player player = user.getPlayer();
            if (player != null)
                player.setInvulnerable(this.immune = immune);
        }

        @Override
        public void giveImmunity(int seconds) {
            if (seconds == 0) {
                if (immune) {
                    cancelTask();
                    setImmune(false);
                }
                return;
            }

            if (seconds < 0) {
                cancelTask();
                if (!immune) setImmune(true);
                return;
            }

            setImmune(true);
            task = plugin.getScheduler().runTaskLater(() -> setImmune(false), seconds * 20L);
        }
    }

    final class ColorImpl extends BaseData implements ColorData {

        @Getter
        private Set<String> formats = new HashSet<>();

        @Setter
        private String colorStart = "", colorEnd = null;

        ColorImpl(UUID uuid) {
            super(colorFile, uuid.toString());
        }

        @NotNull
        public String getStart() {
            return colorStart + String.join("", formats);
        }

        @Nullable
        public String getEnd() {
            return colorEnd;
        }

        @Override
        public void removeAnyFormats() {
            colorStart = "";
            colorEnd = null;
            formats = new HashSet<>();
        }

        void load() {
            if (file == null) return;

            colorStart = file.get(uuid + ".start", colorStart);
            formats = new HashSet<>(file.toStringList(uuid + ".formats"));

            colorEnd = file.get(uuid + ".end", colorEnd);
            if (StringUtils.isBlank(colorEnd)) colorEnd = null;
        }

        void save(boolean save) {
            if (file == null) return;

            file.set(uuid + ".start", getStart());
            file.set(uuid + ".formats", new ArrayList<>(formats));
            file.set(uuid + ".end", colorEnd);
            if (save) file.save();
        }
    }

    @Getter
    abstract class BaseUser implements SIRUser {

        private final IgnoreImpl ignoreData;
        private final MuteImpl muteData;
        private final ChannelImpl channelData;
        private final ColorImpl colorData;
        private final ImmuneImpl immuneData;

        @Getter
        final UUID uuid;

        @Setter
        private boolean logged = false;

        BaseUser(UUID uuid) {
            this.uuid = uuid;

            ignoreData = new IgnoreImpl(uuid);
            muteData = new MuteImpl(this);
            channelData = new ChannelImpl(uuid);
            colorData = new ColorImpl(uuid);
            immuneData = new ImmuneImpl(this);
        }

        @NotNull
        public abstract String getName();

        @Nullable
        public String getPrefix() {
            Player player = getPlayer();
            return player == null ? null : plugin.getChat().getPrefix(player);
        }

        @Nullable
        public String getSuffix() {
            Player player = getPlayer();
            return player == null ? null : plugin.getChat().getSuffix(player);
        }

        @Override
        public boolean hasPermission(String permission) {
            Player player = getPlayer();
            return player != null && plugin.getUserManager().hasPermission(player, permission);
        }

        @Override
        public boolean isLogged() {
            return !plugin.getModuleManager().isEnabled("Login") || logged;
        }

        @Override
        public boolean isVanished() {
            if (!plugin.getModuleManager().isEnabled("Vanish")) return false;

            Player player = getPlayer();
            if (player == null) return false;

            PluginManager manager = plugin.getServer().getPluginManager();

            Plugin essentials = manager.getPlugin("Essentials");
            if (essentials != null)
                return ((Essentials) essentials).getUser(player).isVanished();

            if (manager.getPlugin("CMI") != null)
                return CMIUser.getUser(player).isVanished();

            for (MetadataValue meta : player.getMetadata("vanished"))
                if (meta.asBoolean()) return true;

            return false;
        }

        @NotNull
        public Set<SIRUser> getNearbyUsers(double range) {
            Player player = getPlayer();
            if (player == null) return Collections.emptySet();

            Location location = player.getLocation();
            Set<SIRUser> users = new HashSet<>();

            final World world = location.getWorld();
            if (world == null) return users;

            world.getPlayers().forEach(p -> {
                if (p.getLocation().distance(location) <= range) {
                    SIRUser nearby = getUser(p);
                    if (nearby != null) users.add(nearby);
                }
            });

            return users;
        }

        void load() {
            ignoreData.load();
            muteData.load();
            channelData.load();
            colorData.load();
        }

        void save(boolean save) {
            ignoreData.save(save);
            muteData.save(save);
            channelData.save(save);
            colorData.save(save);
        }
    }

    @Getter
    final class OnlineUser extends BaseUser {

        @NotNull
        private final OfflinePlayer offline;
        @NotNull
        private final Player player;
        private final String name;

        OnlineUser(Player player) {
            super(player.getUniqueId());
            this.offline = Bukkit.getOfflinePlayer(uuid);
            this.player = player;
            this.name = this.player.getName();
        }

        @Override
        public boolean isOnline() {
            return true;
        }

        @NotNull
        public OfflinePlayer getOffline() {
            return offline;
        }

        @NotNull
        public Player getPlayer() {
            return player;
        }

        @NotNull
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "OnlineUser{player=" + player + '}';
        }
    }

    @Getter
    final class OfflineUser extends BaseUser {

        private final OfflinePlayer offline;
        private final Player player;

        OfflineUser(OfflinePlayer offline) {
            super(offline.getUniqueId());
            this.offline = offline;
            this.player = offline.getPlayer();
        }

        @Override
        public boolean isOnline() {
            return false;
        }

        @NotNull
        public OfflinePlayer getOffline() {
            return offline;
        }

        @Override
        public Player getPlayer() {
            return player;
        }

        @NotNull
        public String getName() {
            if (offline != null && offline.getName() != null)
                return offline.getName();

            if (player != null) return player.getName();
            return "Unknown-" + (uuid != null ? uuid.toString().substring(0, 8) : "null");
        }

        @Override
        public String toString() {
            return "OfflineUser{offline=" + offline + ", name=" + getName() + '}';
        }
    }
}
