package me.croabeast.sir;

import lombok.AccessLevel;
import lombok.Getter;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.CustomListener;
import me.croabeast.common.updater.Platform;
import me.croabeast.common.updater.UpdateChecker;
import me.croabeast.common.updater.UpdateResult;
import me.croabeast.common.updater.VersionScheme;
import me.croabeast.common.util.Exceptions;
import me.croabeast.file.ResourceUtils;
import me.croabeast.common.MetricsLoader;
import me.croabeast.common.util.ServerInfoUtils;
import me.croabeast.scheduler.GlobalScheduler;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.sir.manager.*;
import me.croabeast.sir.misc.DelayLogger;
import me.croabeast.sir.misc.Timer;
import me.croabeast.sir.module.*;
import me.croabeast.takion.TakionLib;
import me.croabeast.takion.VaultHolder;
import me.croabeast.takion.bossbar.AnimatedBossbar;
import me.croabeast.takion.character.SmallCaps;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URLDecoder;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

@Getter
public final class SIRPlugin extends JavaPlugin {

    private static final String JAR_PATH;

    static {
        String path = SIRPlugin.class.getProtectionDomain()
                .getCodeSource()
                .getLocation().getPath();

        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (Exception ignored) {}

        JAR_PATH = path;
    }

    @Getter(AccessLevel.NONE)
    final boolean allowTakionMetrics = true;

    @Getter
    private static SIRPlugin instance;
    @Getter
    private static GlobalScheduler scheduler;
    @Getter
    private static TakionLib lib;

    @Getter
    private static String version, author;

    @Getter(AccessLevel.NONE)
    private UserManagerImpl userManager;
    private VaultHolder<?> vaultHolder;

    private CommandManager commandManager;
    private ModuleManager moduleManager;
    private WorldRuleManager worldRuleManager;

    @Override
    public void onEnable() {
        final Timer initializer = Timer.create(true);

        instance = this;
        scheduler = GlobalScheduler.getScheduler(this);

        author = getDescription().getAuthors().get(0);
        version = getDescription().getVersion();

        DelayLogger files = FileData.loadFiles();

        userManager = new UserManagerImpl(this);
        userManager.register();

        worldRuleManager = new RuleManagerImpl(this);
        worldRuleManager.load();

        moduleManager = new ModuleImpl(this);
        moduleManager.load();
        moduleManager.register();

        vaultHolder = VaultHolder.loadHolder();
        lib = new LangUtils(this);
        FileData.FILE_MAP.forEach((k, v) -> v.setLoggerAction(lib.getLogger()::log));

        commandManager = new CommandImpl(this);
        commandManager.load();
        commandManager.register();

        final DelayLogger logger = new DelayLogger(lib);
        logger.add(true,
                "===================================",
                "&0 * &6____ &0* &6___ &0* &6____",
                "&0* &6(___&0 * * &6|&0* * &6|___)",
                "&0* &6____) . _|_ . | &0* &6\\ . &f" + version,
                "      &f&oDeveloped by " + author,
                "===================================",
                "&e[Server]",
                "- Fork & Version: " + ServerInfoUtils.SERVER_FORK,
                "- Java Version: " + ServerInfoUtils.JAVA_VERSION
        );

        logger.add(files).add(true, "&e[Modules]");

        CollectionBuilder<SIRModule> modules = moduleManager.asBuilder();
        String modulesCount = "/" + modules.size();

        logger.add(true,
                "- Loaded: " + modules.size() + modulesCount,
                "- Enabled: " + modules.sizeByFilter(SIRModule::isEnabled) + modulesCount,
                "- Registered: " + modules.sizeByFilter(SIRModule::isRegistered) + modulesCount
        );

        logger.add(true, "&e[Commands]");

        CollectionBuilder<SIRCommand> commands = commandManager.asBuilder();
        String commandsCount = "/" + commands.size();
        logger.add(true,
                "- Loaded: " + commands.size() + commandsCount,
                "- Enabled: " + commands.sizeByFilter(SIRCommand::isEnabled) + commandsCount,
                "- Registered: " + commands.sizeByFilter(SIRCommand::isRegistered) + commandsCount
        );

        logger.add(true, "&e[Hooks]");
        int hooksEnabled = 0;

        Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (papi != null && papi.isEnabled()) {
            logger.add("- " + papi.getName() + ": " + papi.getDescription().getVersion(), true);
            hooksEnabled++;
        }

        Function<Plugin, String> function = plugin ->
                plugin.getName() + " " + plugin.getDescription().getVersion();

        Plugin permission = vaultHolder.getPlugin();
        if (permission != null) {
            logger.add("- Permission: &e" + function.apply(permission), true);
            hooksEnabled++;
        }

        Plugin discord = moduleManager
                .fromParent(SIRModule.Key.DISCORD, HookLoadable::getHookedPlugin);
        Plugin login = moduleManager
                .fromParent(SIRModule.Key.LOGIN, HookLoadable::getHookedPlugin);
        Plugin vanish = moduleManager
                .fromParent(SIRModule.Key.VANISH, HookLoadable::getHookedPlugin);

        if (discord != null) {
            logger.add("- Discord: &e" + function.apply(discord), true);
            if (discord.getName().matches("EssentialsDiscord"))
                logger.add(true,
                        "  &cWARNING: &7EssentialsDiscord manage all its settings",
                        "           &7in their config files.",
                        "           &7SIR doesn't manage any of them."
                );
            hooksEnabled++;
        }

        if (login != null) {
            logger.add("- Login: &e" + function.apply(login), true);
            hooksEnabled++;
        }

        if (vanish != null) {
            logger.add("- Vanish: &e" + function.apply(vanish), true);
            hooksEnabled++;
        }

        if (hooksEnabled < 1)
            logger.add("- &cNo hooks found. &7Skipping...", true);

        MetricsLoader.initialize(this, 25264)
                .addSimplePie("hasPAPI", Exceptions.isPluginEnabled("PlaceholderAPI"))
                .addSimplePie("hasInteractive", Exceptions.isPluginEnabled("InteractiveChat"))
                .addDrillDownPie(
                        "permissionPlugins", "Permission Plugins",
                        permission != null ? permission.getName() : "None/Other"
                )
                .addDrillDownPie(
                        "discordPlugins", "Discord Plugins",
                        discord != null ? discord.getName() : "None/Other"
                )
                .addDrillDownPie(
                        "loginPlugins", "Login Plugins",
                        login != null ? login.getName() : "None/Other"
                )
                .addDrillDownPie(
                        "vanishPlugins", "Vanish Plugins",
                        vanish != null ? vanish.getName() : "None/Other"
                );

        for (OfflinePlayer o : getServer().getOfflinePlayers())
            if (o != null) userManager.loadData(o);

        for (Player player : getServer().getOnlinePlayers())
            if (player != null) userManager.loadData(player);

        if (SIRModule.Key.LOGIN.isEnabled())
            userManager.getOnlineUsers().forEach(u -> u.setLogged(true));

        logger.add(true,
                "&e[Status]", "- SIR initialized completely.",
                "- Loading time: " + initializer.result() + " ms",
                "==================================="
        ).sendLines();

        startUpdaterChecks();
    }

    interface UpdateDisplay {
        void display(String... strings);
    }

    static final VersionScheme SIR_SCHEME = new VersionScheme() {

        String[] splitVersionInfo(String version) {
            Matcher matcher = Pattern.compile("\\d+(?:\\.\\d+)*").matcher(version);
            return matcher.find() ? matcher.group().split("[.]") : new String[] {"0"};
        }

        @Override
        public String compare(@NotNull String first, @NotNull String second) {
            String[] firstSplit = splitVersionInfo(first);
            String[] secondSplit = splitVersionInfo(second);

            for (int i = 0; i < Math.min(firstSplit.length, secondSplit.length); i++) {
                int currentValue = NumberUtils.toInt(firstSplit[i]),
                        newestValue = NumberUtils.toInt(secondSplit[i]);

                if (newestValue > currentValue) return second;
                else if (newestValue < currentValue) return first;
            }

            return (secondSplit.length > firstSplit.length) ? second : first;
        }
    };

    void startUpdaterChecks() {
        final UpdateChecker updater = UpdateChecker.of(this, SIR_SCHEME);

        BiConsumer<Player, UpdateResult> consumer = (player, result) -> {
            String latest = result.getLatest(), current = result.getLocal();
            String prefix = SmallCaps.toSmallCaps("[updater]");

            UpdateDisplay display = player == null ? lib.getLogger()::log :
                    strings -> {
                        if (UserManager.hasPermission(player, "sir.admin.update"))
                            lib.getLoadedSender()
                                    .setTargets(player).setLogger(false).send(strings);
                    };

            switch (result.getReason()) {
                case NEW_UPDATE:
                    display.display(
                            prefix + " &8» &eUpdate Available!",
                            prefix + " &7A new version of SIR was found, please download it.",
                            prefix + " &7Remember, old versions won't receive any support.",
                            prefix + " &7New version:" +
                                    " &6" + latest + "&7, Current version: " + current,
                            prefix + " &7Link:&b https://www.spigotmc.org/resources/96378/"
                    );
                    break;
                case UP_TO_DATE:
                    break;
                case UNRELEASED_VERSION:
                    display.display(
                            prefix + " &8» &aDevelopment build found!",
                            prefix + " &7This version of SIR seems to be on development.",
                            prefix + " &7Errors, bugs and/or inconsistencies might occur.",
                            prefix + " &7Current version:" +
                                    " &6" + current + "&7, Latest version: " + latest
                    );
                    break;
                default:
                    final Throwable throwable = result.getThrowable();
                    display.display(
                            prefix + " &7Not able to verify any checks for updates from Takion.",
                            prefix + " &7Reason: &c" + result.getReason()
                    );
                    if (throwable != null) throwable.printStackTrace();
                    break;
            }
        };

        scheduler.runTaskLater(() -> {
            if (FileData.Main.CONFIG.getFile().get("updater.on-start", true))
                updater.requestCheck(96378, Platform.SPIGOT)
                        .whenComplete((result, e) -> consumer.accept(null, result));
        }, 5);

        new CustomListener() {
            @Getter
            private final Status status = new Status();

            @EventHandler(priority = EventPriority.HIGHEST)
            private void onJoin(PlayerJoinEvent event) {
                if (!FileData.Main.CONFIG.getFile().get("updater.send-op", true))
                    return;

                updater.requestCheck(96378, Platform.SPIGOT)
                        .whenComplete((r, e) ->
                                consumer.accept(event.getPlayer(), r));
            }
        }.register(this);
    }

    @Override
    public void onDisable() {
        final Timer initializer = Timer.create(true);

        final DelayLogger logger = new DelayLogger(lib);
        logger.add(true,
                "===================================",
                "&0 * &6____ &0* &6___ &0* &6____",
                "&0* &6(___&0 * * &6|&0* * &6|___)",
                "&0* &6____) . _|_ . | &0* &6\\ . &f" + version,
                "      &f&oDeveloped by " + author,
                "==================================="
        );

        AnimatedBossbar.unregisterAll();

        moduleManager.unload();
        commandManager.unload();

        userManager.unregister();
        userManager.saveAllData();

        scheduler.cancelAll();
        HandlerList.unregisterAll(this);

        logger.add(true,
                "SIR disabled completely in " + initializer.result() + " ms.",
                "==================================="
        ).sendLines();

        lib = null;
        instance = null;
    }

    @NotNull
    public UserManager getUserManager() {
        return userManager;
    }

    public TakionLib getLibrary() {
        return lib;
    }

    public File fileFrom(String... childPaths) {
        return ResourceUtils.fileFrom(getDataFolder(), childPaths);
    }

    private boolean checkDefaultBukkitMethods() {
        return FileData.Main.CONFIG.getFile().get("options.default-bukkit-plugin-methods", false);
    }

    @NotNull
    public FileConfiguration getConfig() {
        if (checkDefaultBukkitMethods()) return super.getConfig();
        throw new IllegalStateException("Please use FileData for File management.");
    }

    @Override
    public void reloadConfig() {
        if (checkDefaultBukkitMethods()) {
            super.reloadConfig();
            return;
        }
        throw new IllegalStateException("Please use FileData for File management.");
    }

    @Override
    public void saveConfig() {
        if (checkDefaultBukkitMethods()) {
            super.saveConfig();
            return;
        }
        throw new IllegalStateException("Please use FileData for File management.");
    }

    @Nullable
    public PluginCommand getCommand(@NotNull String name) {
        if (checkDefaultBukkitMethods()) return super.getCommand(name);
        throw new IllegalStateException("Please refer to SIRPlugin#getCommandManager() for command management.");
    }

    ClassLoader classLoader() {
        return getClassLoader();
    }

    static CollectionBuilder<String> getJarEntries() {
        try (JarFile jar = new JarFile(JAR_PATH)) {
            return CollectionBuilder.of(jar.entries()).map(ZipEntry::getName);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
