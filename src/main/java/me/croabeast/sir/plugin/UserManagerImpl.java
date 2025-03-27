package me.croabeast.sir.plugin;

import com.Zrips.CMI.Containers.CMIUser;
import com.earth2me.essentials.Essentials;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.Registrable;
import me.croabeast.lib.file.ConfigurableFile;
import me.croabeast.lib.util.Exceptions;
import me.croabeast.sir.api.CustomListener;
import me.croabeast.sir.plugin.manager.UserManager;
import me.croabeast.sir.plugin.module.SIRModule;
import me.croabeast.sir.plugin.misc.SIRUser;
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
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

final class UserManagerImpl implements UserManager, Registrable {

    private final Map<UUID, BaseUser> userMap = new HashMap<>();

    private final SIRPlugin plugin;
    private final CustomListener listener;

    UserManagerImpl(SIRPlugin plugin) {
        this.plugin = plugin;

        listener = new CustomListener() {
            @Getter @Setter
            private boolean registered = false;

            @EventHandler(priority = EventPriority.LOWEST)
            void onJoin(PlayerJoinEvent event) {
                loadData(event.getPlayer());
            }

            @EventHandler(priority = EventPriority.HIGHEST)
            void onQuit(PlayerQuitEvent event) {
                saveData(event.getPlayer());
            }
        };
    }

    BaseUser asBase(SIRUser user) {
        return (BaseUser) user;
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
        user = userMap.get(user.getUuid());

        user.getIgnoreData().load();
        user.getMuteData().load();
        user.getChatData().load();
    }

    void loadData(Player player) {
        BaseUser before = new OnlineUser(player);
        UUID uuid = before.getUuid();

        BaseUser user = userMap.get(uuid);
        if (user == null || user instanceof OfflineUser) {
            userMap.put(uuid, before);
            user = userMap.get(uuid);
        }

        user.getIgnoreData().load();
        user.getMuteData().load();
        user.getChatData().load();
    }

    void saveData(OfflinePlayer player) {
        BaseUser user = userMap.remove(player.getUniqueId());
        user.giveImmunity(0);

        user.getIgnoreData().save();
        user.getMuteData().save();
        user.getChatData().save();
    }

    void saveAllData() {
        ConfigurableFile ignore = FileData.Command.Multi.IGNORE.getFile(false);
        ConfigurableFile mute = FileData.Command.Multi.MUTE.getFile(false);
        ConfigurableFile chat = FileData.Command.Multi.CHAT_VIEW.getFile(false);

        int ignoreSaves = 0, muteSaves = 0, chatSaves = 0;

        for (OfflinePlayer player : plugin.getServer().getOfflinePlayers()) {
            BaseUser user = userMap.remove(player.getUniqueId());
            String key = user.getUuid().toString();

            user.giveImmunity(0);

            try {
                chat.set(key, new ArrayList<>(user.chatData.disabled));
                chatSaves++;
            } catch (Exception ignored) {}

            try {
                ignore.set(key + ".chat", user.ignoreData.getIgnored(true));
                ignore.set(key + ".msg", user.ignoreData.getIgnored(false));

                ignore.set(key + ".ignoreAllChat", user.ignoreData.chatAll);
                ignore.set(key + ".ignoreAllMsg", user.ignoreData.msgAll);

                ignoreSaves++;
            }
            catch (Exception ignored) {}

            if (user.muteData.taskId != -1) {
                plugin.getServer().getScheduler().cancelTask(user.muteData.taskId);
                user.muteData.taskId = -1;
            }

            try {
                if (!user.muteData.muted) {
                    if (mute.contains(key)) {
                        mute.set(key, null);
                        muteSaves++;
                    }
                } else {
                    mute.set(key + ".muted", true);
                    mute.set(key + ".remaining", (user.muteData.expiresAt > 0) ?
                            user.muteData.expiresAt - System.currentTimeMillis() : -1);
                    mute.set(key + ".expiresAt", user.muteData.expiresAt);
                    muteSaves++;
                }
            }
            catch (Exception ignored) {}
        }

        if (ignoreSaves > 0) ignore.save();
        if (muteSaves > 0) mute.save();
        if (chatSaves > 0) chat.save();
    }

    @Override
    public SIRUser getUser(UUID uuid) {
        return uuid == null ? null : userMap.get(uuid);
    }

    @Override
    public SIRUser getUser(String name) {
        if (StringUtils.isBlank(name)) return null;

        for (SIRUser user : userMap.values())
            if (user.getName().equals(name)) return user;

        return null;
    }

    @Override
    public SIRUser fromClosest(String input) {
        if (StringUtils.isBlank(input)) return null;

        for (SIRUser user : userMap.values())
            if (user.getName().matches("(?i)" + Pattern.quote(input)))
                return user;

        return null;
    }

    @Override
    public void mute(SIRUser user, int seconds, String admin, String reason) {
        if (user != null)
            asBase(user).getMuteData().mute(seconds, admin, reason);
    }

    @Override
    public void unmute(SIRUser user) {
        if (user != null) asBase(user).getMuteData().unmute();
    }

    @Override
    public void toggleLocalChannelView(SIRUser user, String channel) {
        asBase(user).getChatData().toggleView(channel);
    }

    @Override
    public Set<SIRUser> getOfflineUsers() {
        return Collections.unmodifiableSet(new HashSet<>(userMap.values()));
    }

    @Override
    public Set<SIRUser> getOnlineUsers() {
        return Collections
                .unmodifiableSet(CollectionBuilder
                        .of(userMap.values())
                        .filter(SIRUser::isOnline)
                        .toSet());
    }

    static class IgnoreData {

        private final String uuidKey;

        final Set<UUID> chatIgnored = new HashSet<>();
        final Set<UUID> msgIgnored = new HashSet<>();

        boolean msgAll = false;
        boolean chatAll = false;

        private final ConfigurableFile file;

        IgnoreData(UUID uuid) {
            file = FileData.Command.Multi.IGNORE.getFile(false);
            uuidKey = uuid.toString();
        }

        List<String> getIgnored(boolean isChat) {
            final Set<UUID> set = isChat ? chatIgnored : msgIgnored;
            return CollectionBuilder.of(set).map(UUID::toString).toList();
        }

        Set<UUID> setFromData(List<String> list) {
            return CollectionBuilder.of(list).map(UUID::fromString).collect(HashSet::new);
        }

        void load() {
            msgIgnored.clear();
            chatIgnored.clear();

            msgIgnored.addAll(setFromData(file.toStringList(uuidKey + ".msg")));
            chatIgnored.addAll(setFromData(file.toStringList(uuidKey + ".chat")));

            msgAll = file.get(uuidKey + "ignoreAllMsg", false);
            chatAll = file.get(uuidKey + "ignoreAllChat", false);
        }

        void save() {
            file.set(uuidKey + ".chat", getIgnored(true));
            file.set(uuidKey + ".msg", getIgnored(false));

            file.set(uuidKey + ".ignoreAllChat", chatAll);
            file.set(uuidKey + ".ignoreAllMsg", msgAll);

            file.save();
        }

        @Override
        public String toString() {
            return "IgnoreData{uuid='" + uuidKey + "', chatIgnored=" + chatIgnored +
                    ", msgIgnored=" + msgIgnored +
                    ", chatAll=" + chatAll + ", msgAll=" + msgAll + '}';
        }
    }

    class MuteData {

        private final String uuidKey;

        private int taskId = -1;
        private long remaining = 0, expiresAt = 0;

        private boolean muted = false;
        private String admin, reason;

        private final ConfigurableFile file;

        MuteData(UUID uuid) {
            file = FileData.Command.Multi.MUTE.getFile(true);
            uuidKey = uuid.toString();

            admin = file.get("default.admin", "Unknown");
            reason = file.get("default.mute-reason", "Not following server rules.");
        }

        void load() {
            muted = file.get(uuidKey + ".muted", false);
            expiresAt = file.get(uuidKey + ".expiresAt", -1L);
            remaining = file.get(uuidKey + ".remaining", -1L);

            admin = file.get(uuidKey + ".admin", admin);
            reason = file.get(uuidKey + ".reason", reason);

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

        void save() {
            if (taskId != -1) {
                plugin.getServer().getScheduler().cancelTask(taskId);
                taskId = -1;
            }

            boolean saved = false;

            if (!muted) {
                if (file.contains(uuidKey)) {
                    file.set(uuidKey, null);
                    saved = true;
                }
            } else {
                file.set(uuidKey + ".muted", true);
                file.set(uuidKey + ".remaining",
                        (expiresAt > 0) ? expiresAt - System.currentTimeMillis() : -1);

                file.set(uuidKey + ".expiresAt", expiresAt);
                file.set(uuidKey + ".admin", admin);
                file.set(uuidKey + ".reason", reason);
                saved = true;
            }

            if (saved) file.save();
        }

        void mute(int seconds, String admin, String reason) {
            if (seconds == 0) {
                unmute();
                return;
            }

            if (StringUtils.isNotBlank(admin))
                this.admin = admin;

            if (StringUtils.isNotBlank(reason))
                this.reason = reason;

            if (seconds > 0) {
                muted = true;
                remaining = seconds * 1000L;

                if (expiresAt < 1)
                    expiresAt = System.currentTimeMillis() + remaining;

                scheduleUnmute(remaining);
                return;
            }

            muted = true;
            remaining = -1;
            expiresAt = -1;
        }

        void unmute() {
            if (taskId != -1) {
                plugin.getServer().getScheduler().cancelTask(taskId);
                taskId = -1;
            }

            muted = false;
            remaining = 0;
        }

        void scheduleUnmute(long remaining) {
            BukkitScheduler scheduler = plugin.getServer().getScheduler();

            if (taskId != -1) {
                plugin.getServer().getScheduler().cancelTask(taskId);
                taskId = -1;
            }

            taskId = scheduler.runTaskLater(plugin, () -> {
                this.remaining = 0;
                muted = false;
                expiresAt = -1;
            }, remaining / 50L).getTaskId();
        }
    }

    static class ChatViewData {

        private final Set<String> disabled = new HashSet<>();
        private final String uuidKey;

        private final ConfigurableFile channels;
        private final ConfigurableFile data;

        ChatViewData(UUID uuid) {
            uuidKey = uuid.toString();

            channels = FileData.Module.Chat.CHANNELS.getFile();
            data = FileData.Command.Multi.CHAT_VIEW.getFile(false);
        }

        void toggleView(String name) {
            if (StringUtils.isBlank(name)) return;

            ConfigurationSection section = channels.getSection("channels." + name);
            if (section == null || section.getBoolean("global", true)) return;

            if (disabled.contains(name)) {
                disabled.remove(name);
                return;
            }

            disabled.add(name);
        }

        void load() {
            disabled.clear();
            disabled.addAll(data.toStringList(uuidKey));
        }

        void save() {
            data.set(uuidKey, new ArrayList<>(disabled));
            data.save();
        }
    }

    @Getter
    abstract class BaseUser implements SIRUser {

        private final UUID uuid;

        private final IgnoreData ignoreData;
        private final MuteData muteData;
        private final ChatViewData chatData;

        @Getter(AccessLevel.NONE)
        @Setter
        private boolean logged = false;

        private boolean immune = false;
        private int immuneTask = -1;

        BaseUser(UUID uuid) {
            this.uuid = uuid;

            ignoreData = new IgnoreData(uuid);
            muteData = new MuteData(uuid);
            chatData = new ChatViewData(uuid);
        }

        @Nullable
        public String getPrefix() {
            return plugin.getVaultHolder().getPrefix(getPlayer());
        }

        @Nullable
        public String getSuffix() {
            return plugin.getVaultHolder().getSuffix(getPlayer());
        }

        @Override
        public boolean isLogged() {
            return !SIRModule.Key.LOGIN.isEnabled() || logged;
        }

        @Override
        public boolean isVanished() {
            if (!SIRModule.Key.VANISH.isEnabled()) return false;

            PluginManager manager = plugin.getServer().getPluginManager();
            Plugin e = manager.getPlugin("Essentials"),
                    c = manager.getPlugin("CMI");

            if (e != null)
                return ((Essentials) e).getUser(getPlayer()).isVanished();

            if (c != null) return CMIUser.getUser(getPlayer()).isVanished();

            for (MetadataValue meta : getPlayer().getMetadata("vanished"))
                if (meta.asBoolean()) return true;

            return false;
        }

        @Override
        public void ignore(SIRUser user, boolean isChat) {
            if (user != null)
                (isChat ? ignoreData.chatIgnored : ignoreData.msgIgnored).add(user.getUuid());
        }

        @Override
        public void ignore(Player player, boolean isChat) {
            ignore(getUser(player), isChat);
        }

        @Override
        public void ignoreAll(boolean isChat) {
            if (isChat) {
                ignoreData.chatAll = true;
                return;
            }

            ignoreData.msgAll = true;
        }

        @Override
        public void unignore(SIRUser user, boolean isChat) {
            if (user != null)
                (isChat ? ignoreData.chatIgnored : ignoreData.msgIgnored).remove(user.getUuid());
        }

        @Override
        public void unignore(Player player, boolean isChat) {
            unignore(getUser(player), isChat);
        }

        @Override
        public void unignoreAll(boolean isChat) {
            if (isChat) {
                ignoreData.chatAll = false;
                return;
            }

            ignoreData.msgAll = false;
        }

        @Override
        public boolean isIgnoring(SIRUser user, boolean isChat) {
            if (user == null) return false;

            return isChat ?
                    (ignoreData.chatAll || ignoreData.chatIgnored.contains(user.getUuid())) :
                    (ignoreData.msgAll || ignoreData.msgIgnored.contains(user.getUuid()));
        }

        @Override
        public boolean isIgnoring(Player player, boolean isChat) {
            return isIgnoring(getUser(player), isChat);
        }

        @Override
        public boolean isIgnoringAll(boolean isChat) {
            return isChat ? ignoreData.chatAll : ignoreData.msgAll;
        }

        @Override
        public boolean isLocalChannelToggled(String channel) {
            return chatData.disabled.contains(channel);
        }

        @Override
        public boolean isMuted() {
            if (FileData.Main.CONFIG.getFile().get("options.check-mute", false) &&
                    plugin.getCommandManager().isEnabled("mute"))
                return muteData.muted;

            if (Exceptions.isPluginEnabled("AdvancedBan")) {
                final String id = UUIDManager.get().getUUID(getName());
                return PunishmentManager.get().isMuted(id);
            }

            if (Exceptions.isPluginEnabled("Essentials")) {
                Essentials e = JavaPlugin.getPlugin(Essentials.class);
                return e.getUser(getPlayer()).isMuted();
            }

            return Exceptions.isPluginEnabled("CMI") &&
                    CMIUser.getUser(getPlayer()).isMuted();
        }

        @Override
        public long getRemainingMute() {
            return (muteData.expiresAt - System.currentTimeMillis()) / 1000;
        }

        void setImmune(boolean immune) {
            getPlayer().setInvulnerable(this.immune = immune);
        }

        private void cancelTask() {
            if (immuneTask != -1)
                plugin.getServer().getScheduler().cancelTask(immuneTask);
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
            immuneTask = plugin.getServer().getScheduler()
                    .runTaskLater(
                            plugin, () -> setImmune(false),
                            seconds * 20L
                    ).getTaskId();
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
    }

    @Getter
    final class OnlineUser extends BaseUser {

        @Getter(AccessLevel.NONE)
        @NotNull
        private final OfflinePlayer offline;
        @NotNull
        private final Player player;

        private final UUID uuid;
        private final String name;

        OnlineUser(Player player) {
            super(player.getUniqueId());
            this.uuid = player.getUniqueId();

            this.offline = Bukkit.getOfflinePlayer(uuid);
            this.player = player;

            name = this.player.getName();
        }

        @NotNull
        public OfflinePlayer getOfflinePlayer() {
            return offline;
        }

        @Override
        public String toString() {
            return "OnlineUser{player=" + player + '}';
        }
    }

    final class OfflineUser extends BaseUser {

        @NotNull
        private final OfflinePlayer offline;
        @Getter @NotNull
        private final UUID uuid;

        private Player player = null;

        OfflineUser(OfflinePlayer offline) {
            super(offline.getUniqueId());
            this.offline = offline;
            this.uuid = offline.getUniqueId();
        }

        @NotNull
        public OfflinePlayer getOfflinePlayer() {
            return offline;
        }

        @NotNull
        public Player getPlayer() {
            return Objects.requireNonNull(offline.isOnline() && player != null ? (player = offline.getPlayer()) : player);
        }

        @NotNull
        public String getName() {
            return getPlayer().getName();
        }

        @Override
        public String toString() {
            return "OfflineUser{offline=" + offline + '}';
        }
    }
}
