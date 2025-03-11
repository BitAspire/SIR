package me.croabeast.sir.plugin;

import lombok.AccessLevel;
import lombok.Getter;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.file.ResourceUtils;
import me.croabeast.lib.util.ServerInfoUtils;
import me.croabeast.sir.plugin.command.SIRCommand;
import me.croabeast.sir.plugin.hook.*;
import me.croabeast.sir.plugin.manager.*;
import me.croabeast.sir.plugin.misc.DelayLogger;
import me.croabeast.sir.plugin.misc.Timer;
import me.croabeast.sir.plugin.module.*;
import me.croabeast.takion.TakionLib;
import me.croabeast.takion.message.AnimatedBossbar;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URLDecoder;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.jar.JarFile;
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

    @Getter
    private static SIRPlugin instance;
    @Getter
    private static TakionLib lib;

    @Getter
    private static String version, author;

    @Getter(AccessLevel.NONE)
    private UserManagerImpl userManager;

    private ModuleManager moduleManager;
    private CommandManager commandManager;

    private VaultHolder<?> vaultHolder;
    private WorldRuleManager worldRuleManager;

    @Override
    public void onEnable() {
        final Timer initializer = Timer.create(true);

        instance = this;
        author = getDescription().getAuthors().get(0);
        version = getDescription().getVersion();

        vaultHolder = HolderUtils.loadHolder();
        DelayLogger files = FileData.loadFiles();

        userManager = new UserManagerImpl(this);
        userManager.register();

        worldRuleManager = new RuleManagerImpl(this);
        worldRuleManager.load();

        moduleManager = new ModuleManagerImpl(this);
        moduleManager.load();
        moduleManager.register();

        lib = new LangUtils(this);
        FileData.FILE_MAP.forEach((k, v) -> v.setLoggerAction(lib.getLogger()::log));

        commandManager = new CommandManagerImpl(this);
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
        logger.add(true,
                "- Loaded: " + modules.size() + "/13",
                "- Enabled: " + modules.sizeByFilter(SIRModule::isEnabled) + "/13",
                "- Registered: " + modules.sizeByFilter(SIRModule::isRegistered) + "/13"
        );

        logger.add(true, "&e[Commands]");

        CollectionBuilder<SIRCommand> commands = commandManager.asBuilder();
        logger.add(true,
                "- Loaded: " + commands.size() + "/11",
                "- Enabled: " + commands.sizeByFilter(SIRCommand::isEnabled) + "/11",
                "- Registered: " + commands.sizeByFilter(SIRCommand::isRegistered) + "/11"
        );

        logger.add(true, "&e[Hooks]");
        int hooksEnabled = 0;

        UnaryOperator<String> operator = s -> "- " + s + ": " + HookChecker.getVersion(s);

        if (HookChecker.DISCORD_ENABLED) {
            logger.add(operator.apply("DiscordSRV"), true);
            hooksEnabled++;
        }

        if (HookChecker.PAPI_ENABLED) {
            logger.add(operator.apply("PlaceholderAPI"), true);
            hooksEnabled++;
        }

        Function<Plugin, String> function = plugin ->
                plugin.getName() + " " + plugin.getDescription().getVersion();

        Plugin permission = vaultHolder.getPlugin();
        if (permission != null) {
            logger.add("- Permission Plugin: &e" + function.apply(permission), true);
            hooksEnabled++;
        }

        Plugin login = moduleManager
                .fromParent(SIRModule.Key.LOGIN, HookLoadable::getHookedPlugin);
        Plugin vanish = moduleManager
                .fromParent(SIRModule.Key.VANISH, HookLoadable::getHookedPlugin);

        if (login != null) {
            logger.add("- Login Plugin: &e" + function.apply(login), true);
            hooksEnabled++;
        }

        if (vanish != null) {
            logger.add("- Vanish Plugin: &e" + function.apply(vanish), true);
            hooksEnabled++;
        }

        if (hooksEnabled < 1)
            logger.add("- &cNo hooks found. &7Skipping...", true);

        MetricsLoader.initialize(this)
                .addSimplePie("hasVault", HookChecker.VAULT_ENABLED)
                .addSimplePie("hasPAPI", HookChecker.PAPI_ENABLED)
                .addSimplePie("hasDiscord", HookChecker.DISCORD_ENABLED)
                .addDrillDownPie(
                        "loginPlugins", "Login Plugins",
                        login != null ? login.getName() : null,
                        "None / Other"
                )
                .addDrillDownPie(
                        "vanishPlugins", "Vanish Plugins",
                        vanish != null ? vanish.getName() : null,
                        "None / Other"
                );

        for (OfflinePlayer o : Bukkit.getOfflinePlayers()) userManager.loadData(o);

        if (SIRModule.Key.LOGIN.isEnabled())
            userManager.getOnlineUsers().forEach(u -> u.setLogged(true));

        logger.add(true, "&e[Status]",
                "- SIR initialized completely.",
                "- Loading time: " + initializer.result() + " ms",
                "==================================="
        ).sendLines();
    }

    @Override
    public void onDisable() {
        AnimatedBossbar.unregisterAll();

        moduleManager.unload();
        commandManager.unload();

        userManager.unregister();
        userManager.saveAllData();

        lib = null;
        instance = null;
    }

    ClassLoader classLoader() {
        return getClassLoader();
    }

    @NotNull
    public SIRUserManager getUserManager() {
        return userManager;
    }

    public File fileFrom(String... childPaths) {
        return ResourceUtils.fileFrom(getDataFolder(), childPaths);
    }

    public TakionLib getLibrary() {
        return lib;
    }

    public static CollectionBuilder<String> getJarEntries() {
        try (JarFile jar = new JarFile(JAR_PATH)) {
            return CollectionBuilder.of(jar.entries()).map(ZipEntry::getName);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
