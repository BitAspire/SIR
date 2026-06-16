package com.bitaspire.sir.module.scoreboard;

import com.bitaspire.sir.file.ExtensionFile;
import com.bitaspire.sir.command.CommandProvider;
import com.bitaspire.sir.command.SIRCommand;
import com.bitaspire.sir.module.SIRModule;
import lombok.Getter;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.scheduler.GlobalTask;
import me.croabeast.takion.TakionLib;
import me.croabeast.takion.logger.LogLevel;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class ScoreboardModule extends SIRModule implements CommandProvider {

    @Getter
    private final Set<SIRCommand> commands = new HashSet<>();
    private final Map<UUID, ScoreboardSession> sessions = new HashMap<>();
    private final Set<UUID> disabled = new HashSet<>();
    private final Map<String, ScoreboardProfile> profiles = new LinkedHashMap<>();

    private ScoreboardListener listener;
    private GlobalTask task;
    private long tick;
    private String defaultProfile = "default";
    private int animatedUpdateTicks = 5;
    private int staticUpdateTicks = 40;
    private int maxLines = 15;
    private boolean legacySafeSplit = true;
    private boolean updateOnlyWhenNeeded = true;
    private boolean anyAnimated;

    @Override
    public boolean register() {
        reloadFiles();

        try {
            commands.clear();
            commands.add(createCommand(new ExtensionFile(this, "lang", true)));
        } catch (Exception e) {
            getLogger().log(LogLevel.ERROR, "Command 'scoreboard' cannot be loaded because lang.yml is missing.");
            e.printStackTrace();
        }

        listener = new ScoreboardListener(this);
        listener.register();
        startTask();
        refreshAll();
        return true;
    }

    @Override
    public boolean unregister() {
        stopTask();
        if (listener != null) listener.unregister();

        for (Player player : Bukkit.getOnlinePlayers())
            remove(player);

        sessions.clear();
        return true;
    }

    protected ScoreboardCommand createCommand(ConfigurableFile lang) {
        return new ScoreboardCommand(this, lang);
    }

    void reloadFiles() {
        saveDefaults();
        loadConfig();
        loadProfiles();
        restartTask();
        refreshAll();
    }

    Set<String> getProfileNames() {
        return new LinkedHashSet<>(profiles.keySet());
    }

    Collection<ScoreboardProfile> getProfiles() {
        return profiles.values();
    }

    boolean hasProfile(String name) {
        return findProfile(name) != null;
    }

    boolean toggle(Player player) {
        UUID uuid = player.getUniqueId();
        if (disabled.remove(uuid)) {
            refresh(player);
            return true;
        }

        disabled.add(uuid);
        remove(player);
        return false;
    }

    void refreshAll() {
        ScoreboardRefreshContext context = ScoreboardRefreshContext.create(tick);
        for (Player player : Bukkit.getOnlinePlayers())
            refresh(player, context);
    }

    void refresh(Player player) {
        refresh(player, ScoreboardRefreshContext.create(tick));
    }

    void refreshLater(Player player, long delay) {
        getApi().getScheduler().runTaskLater(() -> {
            if (isRegistered() && player != null && player.isOnline()) refresh(player);
        }, delay);
    }

    void refreshAllLater(long delay) {
        getApi().getScheduler().runTaskLater(() -> {
            if (isRegistered()) refreshAll();
        }, delay);
    }

    private void refresh(Player player, ScoreboardRefreshContext context) {
        if (player == null || !player.isOnline()) return;
        if (!isDisplayed(player)) {
            remove(player);
            return;
        }

        ScoreboardProfile profile = resolveProfile(player);
        ScoreboardSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            session = new ScoreboardSession(player);
            sessions.put(player.getUniqueId(), session);
        }

        List<String> lines = profile.lines(player, this, context);
        session.render(player, profile.title(player, this, context), lines, maxLines, legacySafeSplit);
    }

    void remove(Player player) {
        if (player == null) return;

        ScoreboardSession session = sessions.remove(player.getUniqueId());
        if (session != null) session.close(player);
    }

    protected boolean isDisplayed(Player player) {
        return !disabled.contains(player.getUniqueId());
    }

    ScoreboardProfile resolveProfile(Player player) {
        return resolveAutomaticProfile(player);
    }

    ScoreboardProfile resolveAutomaticProfile(Player player) {
        ScoreboardProfile fallback = findProfile(defaultProfile);
        ScoreboardProfile best = null;
        for (ScoreboardProfile candidate : profiles.values()) {
            if (!candidate.matches(this, player)) continue;
            if (best == null || candidate.getPriority() > best.getPriority())
                best = candidate;
        }

        return best != null ? best : fallback != null ? fallback : firstProfile();
    }

    private ScoreboardProfile firstProfile() {
        if (!profiles.isEmpty()) return profiles.values().iterator().next();
        return ScoreboardProfile.from("default", new YamlConfiguration());
    }

    ScoreboardProfile findProfile(String name) {
        if (name == null) return null;
        return profiles.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    void saveDefaults() {
        try {
            new ExtensionFile(this, "config", true);
            new ExtensionFile(this, "profiles", true);
            new ExtensionFile(this, "lang", true);
        } catch (Exception e) {
            TakionLib library = getApi().getLibrary();
            library.getLogger().log(LogLevel.ERROR, "Failed to save Scoreboard module defaults.");
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        YamlConfiguration config = load("config");
        defaultProfile = config.getString("settings.default-profile", "default");
        animatedUpdateTicks = Math.max(1, config.getInt("performance.animated-update-ticks", 5));
        staticUpdateTicks = Math.max(1, config.getInt("performance.static-update-ticks", 40));
        updateOnlyWhenNeeded = config.getBoolean("performance.update-only-when-needed", true);
        legacySafeSplit = config.getBoolean("compatibility.legacy-safe-split", true);
        maxLines = Math.max(1, Math.min(15, config.getInt("compatibility.max-lines", 15)));
    }

    private void loadProfiles() {
        profiles.clear();
        anyAnimated = false;

        ConfigurationSection section = load("profiles").getConfigurationSection("profiles");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection profileSection = section.getConfigurationSection(key);
            if (profileSection == null) continue;

            ScoreboardProfile profile = ScoreboardProfile.from(key, profileSection);
            profiles.put(key, profile);
            anyAnimated |= profile.hasAnimations();
        }
    }

    private YamlConfiguration load(String name) {
        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), name + ".yml"));
    }

    private void startTask() {
        stopTask();

        if (!anyAnimated && updateOnlyWhenNeeded) return;

        int interval = anyAnimated ? animatedUpdateTicks : staticUpdateTicks;
        task = getApi().getScheduler().runTaskTimer(() -> {
            tick += interval;
            refreshAll();
        }, interval, interval);
    }

    private void restartTask() {
        if (isRegistered()) startTask();
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
