package me.croabeast.sir;

import com.Zrips.CMI.Containers.CMIUser;
import com.earth2me.essentials.Essentials;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.CustomListener;
import me.croabeast.common.Registrable;
import me.croabeast.common.util.Exceptions;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.scheduler.GlobalScheduler;
import me.croabeast.scheduler.GlobalTask;
import me.croabeast.sir.manager.UserManager;
import me.croabeast.sir.user.SIRUser;
import me.croabeast.sir.module.SIRModule;
import me.croabeast.sir.user.*;
import me.leoko.advancedban.manager.PunishmentManager;
import me.leoko.advancedban.manager.UUIDManager;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
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

import java.util.*;
import java.util.regex.Pattern;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class UserManagerImpl implements UserManager, Registrable {

    private final Map<UUID, BaseUser> userMap = new HashMap<>();

    private final SIRPlugin plugin;
    private final CustomListener listener;

    private GlobalTask autoSaveTask = null;
    private static final Object SAVE_LOCK = new Object();

    UserManagerImpl(SIRPlugin plugin) {
        this.plugin = plugin;

        listener = new CustomListener() {
            @Getter
            private final Status status = new Status();

            @EventHandler(priority = EventPriority.LOW)
            void onJoin(PlayerJoinEvent event) {
                loadData(event.getPlayer());
            }

            @EventHandler(priority = EventPriority.HIGHEST)
            void onQuit(PlayerQuitEvent event) {
                BaseUser user = userMap.remove(event.getPlayer().getUniqueId());
                if (user == null) return;

                user.getImmuneData().giveImmunity(0);
                user.save(true);
            }
        };
    }

    @Override
    public boolean isRegistered() {
        return listener.isRegistered();
    }

    @Override
    public boolean register() {
        return listener.register();
    }

    @Override
    public boolean unregister() {
        return listener.unregister();
    }

    void loadData(OfflinePlayer offline) {
        BaseUser user = new OfflineUser(offline);
        userMap.put(user.getUuid(), user);
        user.load();
    }

    void loadData(Player player) {
        UUID uuid = player.getUniqueId();

        userMap.values().removeIf(user -> {
            if (user == null) return true;
            try {
                return user.getUuid().equals(uuid) && !user.isOnline();
            } catch (Exception e) {
                return true;
            }
        });

        BaseUser onlineUser = new OnlineUser(player);
        userMap.put(uuid, onlineUser);
        onlineUser.load();
    }

    void removeUuid(Set<ConfigurableFile> files, UUID uuid) {
        BaseUser user = userMap.remove(uuid);
        if (user == null) return;

        user.getImmuneData().giveImmunity(0);
        user.save(false);

        for (BaseData data : user.map.map.values()) {
            ConfigurableFile file = data.file;
            if (file != null) files.add(file);
        }
    }

    void saveAllData() {
        Set<ConfigurableFile> files = new HashSet<>();

        for (Player p : plugin.getServer().getOnlinePlayers())
            removeUuid(files, p.getUniqueId());

        for (OfflinePlayer o : plugin.getServer().getOfflinePlayers())
            removeUuid(files, o.getUniqueId());

        files.forEach(ConfigurableFile::save);
    }

    /**
     * Saves all user data without removing users from the map.
     * This is safe to call during reload operations.
     * Thread-safe implementation.
     */
    @Override
    public void saveAllDataSafely() {
        synchronized (SAVE_LOCK) {
            Set<ConfigurableFile> modifiedFiles = new HashSet<>();

            for (BaseUser user : userMap.values()) {
                if (user == null) continue;

                try {
                    user.save(false);

                    for (BaseData data : user.map.map.values())
                        if (data.file != null) modifiedFiles.add(data.file);
                }
                catch (Exception e) {
                    plugin.getLogger().warning("[UserManager] Failed to save data for user " + user.getUuid() + ": " + e.getMessage());
                }
            }

            for (ConfigurableFile file : modifiedFiles) {
                try {
                    file.save();
                } catch (Exception e) {
                    plugin.getLogger().severe("[UserManager] Failed to save file: " + e.getMessage());
                }
            }

            plugin.getLogger().info("[UserManager] Saved data for " + userMap.size() + " users.");
        }
    }

    /**
     * Starts the auto-save task if enabled in config.
     */
    @Override
    public void startAutoSave() {
        stopAutoSave();

        ConfigurableFile config = FileData.Main.CONFIG.getFile();
        boolean enabled = config.get("options.auto-save.enabled", false);

        if (!enabled) {
            plugin.getLogger().info("[UserManager] Auto-save is disabled.");
            return;
        }

        int min = config.get("options.auto-save.interval-minutes", 5);
        if (min < 1) min = 1;

        long ticks = min * 60L * 20L;

        autoSaveTask = SIRPlugin.getScheduler().runTaskTimer(this::performAutoSave, ticks, ticks);
        plugin.getLogger().info("[UserManager] Auto-save enabled. Interval: " + min + " minutes.");
    }

    /**
     * Stops the auto-save task if running.
     */
    @Override
    public void stopAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
    }

    /**
     * Performs the auto-save operation with logging.
     */
    private void performAutoSave() {
        try {
            saveAllDataSafely();
            if (FileData.Main.CONFIG.getFile().get("options.auto-save.log", true)) {
                plugin.getLogger().info("[UserManager] Auto-save completed successfully.");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[UserManager] Auto-save failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets the Runnable for safe reload operations.
     * @return Runnable that saves all user data safely
     */
    @Override
    public Runnable getSaveRunnable() {
        return this::saveAllDataSafely;
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
            if (user == null) continue;

            try {
                if (user.isOnline()) {
                    String userName = Objects.requireNonNull(user.getName());
                    if (userName.equals(name)) return user;
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    @Override
    public SIRUser fromClosest(String input) {
        if (StringUtils.isBlank(input)) return null;

        for (SIRUser user : userMap.values()) {
            if (user == null) continue;

            try {
                if (user.isOnline()) {
                    String userName = Objects.requireNonNull(user.getName());
                    if (userName.matches("(?i)" + Pattern.quote(input)))
                        return user;
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    @NotNull
    public Set<SIRUser> getOfflineUsers() {
        return Collections.unmodifiableSet(new HashSet<>(userMap.values()));
    }

    @NotNull
    public Set<SIRUser> getOnlineUsers() {
        return Collections.unmodifiableSet(CollectionBuilder.of(userMap.values()).filter(SIRUser::isOnline).toSet());
    }

    @RequiredArgsConstructor
    abstract static class BaseData {

        final ConfigurableFile file;
        final String uuid;

        abstract void load();

        abstract void save(boolean save);
    }

    final class IgnoreImpl extends BaseData implements IgnoreData {

        class UuidSet {

            final Set<UUID> set = new HashSet<>();

            void setAll(Collection<String> collection) {
                if (collection == null) return;

                set.clear();
                set.addAll(CollectionBuilder.of(collection).map(UUID::fromString).toSet());
            }

            List<String> asStrings() {
                return CollectionBuilder.of(set).map(UUID::toString).toList();
            }

            @Override
            public String toString() {
                return set.toString();
            }
        }

        final UuidSet chatSet = new UuidSet(), msgSet = new UuidSet();
        boolean chatAll = false, msgAll = false;

        IgnoreImpl(UUID uuid) {
            super(FileData.Command.Multi.IGNORE.getFile(false), uuid.toString());
        }

        @Override
        public boolean isIgnoring(SIRUser player, boolean chat) {
            return (chat ? chatSet : msgSet).set.contains(player.getUuid());
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

        @Override
        void load() {
            chatSet.setAll(file.toStringList(uuid + ".chat.single"));
            msgSet.setAll(file.toStringList(uuid + ".msg.single"));
            chatAll = file.get(uuid + ".chat.all", false);
            msgAll = file.get(uuid + ".msg.all", false);
        }

        @Override
        void save(boolean save) {
            file.set(uuid + ".chat.single", chatSet.asStrings());
            file.set(uuid + ".msg.single", msgSet.asStrings());
            file.set(uuid + ".chat.all", chatAll);
            file.set(uuid + ".msg.all", msgAll);

            if (save) file.save();
        }
    }

    final class MuteImpl extends BaseData implements MuteData {

        private final SIRUser user;

        private int taskId = -1;
        private long remaining = 0, expiresAt = 0;

        private boolean muted = false;
        @Getter
        private String muteBy, reason;

        MuteImpl(SIRUser user) {
            super(FileData.Command.Multi.MUTE.getFile(false), user.getUuid().toString());
            this.user = user;

            muteBy = file.get("default.admin", "Unknown");
            reason = file.get("default.mute-reason", "Not following server rules.");
        }

        @Override
        public boolean isMuted() {
            if (FileData.Main.CONFIG.getFile().get("options.check-mute", false)
                    && plugin.getCommandManager().isEnabled("mute"))
                return muted;

            if (Exceptions.isPluginEnabled("Essentials")) {
                Essentials e = JavaPlugin.getPlugin(Essentials.class);
                return e.getUser(user.getPlayer()).isMuted();
            }

            if (Exceptions.isPluginEnabled("AdvancedBan")) {
                final String id = UUIDManager.get().getUUID(user.getName());
                return PunishmentManager.get().isMuted(id);
            }

            return Exceptions.isPluginEnabled("CMI") &&
                    CMIUser.getUser(user.getPlayer()).isMuted();
//            return muted;
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
            if (taskId != -1) {
                SIRPlugin.getScheduler().cancel(taskId);
                taskId = -1;
            }

            muted = false;
            remaining = 0;
        }

        @Override
        public long getRemaining() {
            return (expiresAt - System.currentTimeMillis()) / 1000;
        }

        void scheduleUnmute(long remaining) {
            GlobalScheduler scheduler = SIRPlugin.getScheduler();

            if (taskId != -1) {
                scheduler.cancel(taskId);
                taskId = -1;
            }

            taskId = scheduler.runTaskLater(() -> {
                this.remaining = 0;
                muted = false;
                expiresAt = -1;
            }, remaining / 50L).getTaskId();
        }

        @Override
        void load() {
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

        @Override
        void save(boolean save) {
            if (taskId != -1) {
                SIRPlugin.getScheduler().cancel(taskId);
                taskId = -1;
            }

            boolean saved = false;

            if (!muted) {
                if (file.contains(uuid)) {
                    file.set(uuid, null);
                    saved = true;
                }
            } else {
                file.set(uuid + ".muted", true);
                file.set(uuid + ".remaining",
                        (expiresAt > 0) ? expiresAt - System.currentTimeMillis() : -1);

                file.set(uuid + ".expiresAt", expiresAt);
                file.set(uuid + ".admin", muteBy);
                file.set(uuid + ".reason", reason);
                saved = true;
            }

            if (saved && save) file.save();
        }
    }

    static final class ChannelImpl extends BaseData implements ChannelData {

        private final Set<String> toggledChannels = new HashSet<>();
        private final ConfigurableFile channels;

        ChannelImpl(UUID uuid) {
            super(FileData.Command.Multi.CHAT_VIEW.getFile(false), uuid.toString());
            channels = FileData.Module.Chat.CHANNELS.getFile();
        }

        void load() {
            toggledChannels.clear();
            toggledChannels.addAll(file.toStringList(uuid));
        }

        void save(boolean save) {
            file.set(uuid, new ArrayList<>(toggledChannels));
            if (save) file.save();
        }

        @Override
        public void toggle(String channel) {
            if (StringUtils.isBlank(channel)) return;

            ConfigurationSection section = channels.getSection("channels." + channel);
            if (section == null || section.getBoolean("global", true)) return;

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

    static final class ImmuneImpl extends BaseData implements ImmuneData {

        private final SIRUser user;

        @Getter
        private boolean immune = false;
        private int taskId = -1;

        ImmuneImpl(SIRUser user) {
            super(null, user.getUuid().toString());
            this.user = user;
        }

        private void cancelTask() {
            if (taskId != -1)
                SIRPlugin.getScheduler().cancel(taskId);
        }

        void setImmune(boolean immune) {
            user.getPlayer().setInvulnerable(this.immune = immune);
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
            taskId = SIRPlugin.getScheduler()
                    .runTaskLater(
                            () -> setImmune(false),
                            seconds * 20L
                    ).getTaskId();
        }

        @Override
        void load() {}

        @Override
        void save(boolean save) {}
    }

    static final class ColorImpl extends BaseData implements ColorData {

        @Getter
        private Set<String> formats = new HashSet<>();

        @Setter
        private String colorStart = "", colorEnd = null;

        ColorImpl(UUID uuid) {
            super(FileData.Command.Multi.CHAT_COLOR.getFile(false), uuid.toString());
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

        @Override
        void load() {
            colorStart = file.get(uuid + ".start", colorStart);
            formats = new HashSet<>(file.toStringList(uuid + ".formats"));

            colorEnd = file.get(uuid + ".end", colorEnd);
            if (StringUtils.isBlank(colorEnd)) colorEnd = null;
        }

        @Override
        void save(boolean save) {
            file.set(uuid + ".start", getStart());
            file.set(uuid + ".end", colorEnd);
            if (save) file.save();
        }
    }

    abstract class BaseUser implements SIRUser {

        final class DataMap {

            final Map<String, BaseData> map = new HashMap<>();

            @SuppressWarnings("unchecked")
            <D extends BaseData> D get(String name) {
                return (D) map.get(name);
            }
        }

        private final DataMap map = new DataMap();

        @Getter
        final UUID uuid;

        @Setter
        private boolean logged = false;

        BaseUser(UUID uuid) {
            this.uuid = uuid;

            map.map.put("ignore", new IgnoreImpl(uuid));
            map.map.put("mute", new MuteImpl(this));
            map.map.put("channel", new ChannelImpl(uuid));
            map.map.put("color", new ColorImpl(uuid));
            map.map.put("immune", new ImmuneImpl(this));
        }

        @NotNull
        public abstract String getName();

        @Nullable
        public String getPrefix() {
            return plugin.getChat().getPrefix(getPlayer());
        }

        @Nullable
        public String getSuffix() {
            return plugin.getChat().getSuffix(getPlayer());
        }

        @Override
        public boolean isLogged() {
            return !SIRModule.Key.LOGIN.isEnabled() || logged;
        }

        @Override
        public boolean isVanished() {
            if (!SIRModule.Key.VANISH.isEnabled()) return false;

            PluginManager manager = plugin.getServer().getPluginManager();

            Plugin e = manager.getPlugin("Essentials");
            if (e != null)
                return ((Essentials) e).getUser(getPlayer()).isVanished();

            if (manager.getPlugin("CMI") != null)
                return CMIUser.getUser(getPlayer()).isVanished();

            for (MetadataValue meta : getPlayer().getMetadata("vanished"))
                if (meta.asBoolean()) return true;

            return false;
        }

        @NotNull
        public IgnoreData getIgnoreData() {
            return map.get("ignore");
        }

        @NotNull
        public MuteData getMuteData() {
            return map.get("mute");
        }

        @NotNull
        public ChannelData getChannelData() {
            return map.get("channel");
        }

        @NotNull
        public ColorData getColorData() {
            return map.get("color");
        }

        @NotNull
        public ImmuneData getImmuneData() {
            return map.get("immune");
        }

        @NotNull
        public Set<SIRUser> getNearbyUsers(double range) {
            Location location = getPlayer().getLocation();
            Set<SIRUser> users = new HashSet<>();

            final World world = location.getWorld();
            if (world == null) return users;

            world.getPlayers().forEach(p -> {
                if (p.getLocation().distance(location) <= range)
                    users.add(getUser(p));
            });

            return users;
        }

        void load() {
            map.map.values().forEach(BaseData::load);
        }

        void save(boolean save) {
            map.map.values().forEach(data -> data.save(save));
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

            name = this.player.getName();
        }

        @Override
        public boolean isOnline() {
            return true;
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
