package com.bitaspire.sir;

import com.bitaspire.sir.file.Config;
import lombok.Getter;
import me.croabeast.takion.logger.LogLevel;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class StartupDiagnostics implements RuntimeDiagnostics {

    private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)&[0-9a-fk-or]|</?[^>]+>");

    private final SIRPlugin plugin;
    private final boolean saveDetails;
    private final boolean latestFolder;
    private final int maxSessions;
    private final String consoleMode;
    private final String sessionName;

    private final List<String> moduleLines = new ArrayList<>();
    private final List<String> commandLines = new ArrayList<>();
    private final List<String> integrationLines = new ArrayList<>();
    private final List<String> fullLines = new ArrayList<>();
    private final List<String> moduleRequirements = new ArrayList<>();
    private final List<String> commandRequirements = new ArrayList<>();

    @Getter
    private boolean collecting = true;

    private StartupDiagnostics(SIRPlugin plugin, ConfigImpl config) {
        this.plugin = plugin;
        saveDetails = config.isStartupLogDetails();
        latestFolder = config.isStartupLogLatestFolder();
        maxSessions = Math.max(0, config.getStartupLogMaxSessions());
        consoleMode = config.getStartupLogConsole();
        sessionName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ENGLISH).format(new Date());
    }

    static StartupDiagnostics create(SIRPlugin plugin) {
        Config config = plugin.getConfiguration();
        ConfigImpl impl = config instanceof ConfigImpl ? (ConfigImpl) config : new ConfigImpl(plugin);
        return new StartupDiagnostics(plugin, impl);
    }

    public boolean notLogToConsole(LogLevel level) {
        return !isVerboseConsole() && level != LogLevel.ERROR;
    }

    boolean hasDetails() {
        return saveDetails;
    }

    String getDetailPath() {
        if (!saveDetails) return null;
        return latestFolder ? "logs/latest/" : "logs/" + sessionName + "/";
    }

    public void module(LogLevel level, String... messages) {
        record("Modules", moduleLines, level, messages);
    }

    public void command(LogLevel level, String... messages) {
        record("Commands", commandLines, level, messages);
    }

    void integration(String line) {
        if (line == null || line.trim().isEmpty()) return;

        String plain = strip(line);
        integrationLines.add(plain);
        fullLines.add("[Integrations] " + plain);
    }

    public void moduleRequirement(String moduleName, String[] dependencies) {
        addRequirement(moduleRequirements, "Module", moduleName, dependencies);
    }

    public void commandRequirement(String providerName, String[] dependencies) {
        addRequirement(commandRequirements, "Provider", providerName, dependencies);
    }

    void write(
            List<String> summary,
            List<String> moduleSnapshot,
            List<String> commandSnapshot,
            List<String> integrationSnapshot,
            List<String> json
    ) {
        collecting = false;
        if (!saveDetails) return;

        File logsDir = new File(plugin.getDataFolder(), "logs");
        File sessionDir = uniqueSessionDir(logsDir);

        try {
            if (!sessionDir.exists() && !sessionDir.mkdirs())
                throw new IOException("Could not create " + sessionDir.getPath());

            writeFile(new File(sessionDir, "summary.log"), stripAll(summary));
            writeFile(new File(sessionDir, "modules.log"), section("Modules", moduleSnapshot, moduleRequirements, moduleLines));
            writeFile(new File(sessionDir, "commands.log"), section("Commands", commandSnapshot, commandRequirements, commandLines));
            writeFile(new File(sessionDir, "integrations.log"), integrations(integrationSnapshot));
            writeFile(new File(sessionDir, "startup-full.log"), full(summary, moduleSnapshot, commandSnapshot, integrationSnapshot));
            writeFile(new File(sessionDir, "diagnostics.json"), json);

            if (latestFolder) refreshLatest(logsDir, sessionDir);
            pruneSessions(logsDir);
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to write startup diagnostics: " + exception.getMessage());
        }
    }

    private boolean isVerboseConsole() {
        return "verbose".equalsIgnoreCase(consoleMode) || "debug".equalsIgnoreCase(consoleMode);
    }

    private void record(String component, List<String> target, LogLevel level, String... messages) {
        if (messages == null) return;

        for (String message : messages) {
            if (message == null || message.trim().isEmpty()) continue;

            String line = "[" + level + "] " + strip(message);
            target.add(line);
            fullLines.add("[" + component + "] " + line);
        }
    }

    private void addRequirement(List<String> target, String type, String name, String[] dependencies) {
        if (name == null || dependencies == null || dependencies.length == 0) return;

        String line = type + " " + name + " needs " + joinDependencies(dependencies);
        if (!target.contains(line)) target.add(line);
    }

    private String joinDependencies(String[] dependencies) {
        List<String> clean = new ArrayList<>();
        for (String dependency : dependencies) {
            if (dependency != null && !dependency.trim().isEmpty()) clean.add(dependency.trim());
        }

        if (clean.isEmpty()) return "unknown dependency";
        if (clean.size() == 1) return clean.get(0);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < clean.size(); i++) {
            if (i > 0) builder.append(i == clean.size() - 1 ? " or " : ", ");
            builder.append(clean.get(i));
        }
        return builder.toString();
    }

    private List<String> section(String title, List<String> snapshot, List<String> requirements, List<String> events) {
        List<String> lines = new ArrayList<>();
        lines.add(title);
        lines.add(repeat(title.length()));
        lines.addAll(stripAll(snapshot));

        if (!requirements.isEmpty()) {
            lines.add("");
            lines.add("Requirements");
            lines.addAll(requirements);
        }

        if (!events.isEmpty()) {
            lines.add("");
            lines.add("Events");
            lines.addAll(events);
        }

        return lines;
    }

    private List<String> integrations(List<String> snapshot) {
        List<String> lines = section("Integrations", snapshot, new ArrayList<String>(), integrationLines);
        List<String> requirements = new ArrayList<>();
        requirements.addAll(moduleRequirements);
        requirements.addAll(commandRequirements);

        if (!requirements.isEmpty()) {
            lines.add("");
            lines.add("Requirements");
            lines.addAll(requirements);
        }

        return lines;
    }

    private List<String> full(
            List<String> summary,
            List<String> moduleSnapshot,
            List<String> commandSnapshot,
            List<String> integrationSnapshot
    ) {
        List<String> lines = new ArrayList<>();
        lines.add("Summary");
        lines.add("-------");
        lines.addAll(stripAll(summary));
        lines.add("");
        lines.addAll(section("Modules", moduleSnapshot, moduleRequirements, moduleLines));
        lines.add("");
        lines.addAll(section("Commands", commandSnapshot, commandRequirements, commandLines));
        lines.add("");
        lines.addAll(integrations(integrationSnapshot));

        if (!fullLines.isEmpty()) {
            lines.add("");
            lines.add("Raw Events");
            lines.addAll(fullLines);
        }

        return lines;
    }

    private File uniqueSessionDir(File logsDir) {
        File dir = new File(logsDir, sessionName);
        int index = 1;
        while (dir.exists()) {
            dir = new File(logsDir, sessionName + "_" + index);
            index++;
        }
        return dir;
    }

    private void refreshLatest(File logsDir, File sessionDir) throws IOException {
        File latest = new File(logsDir, "latest");
        deleteRecursively(latest);
        copyDirectory(sessionDir, latest);
    }

    private void pruneSessions(File logsDir) {
        if (maxSessions <= 0 || !logsDir.isDirectory()) return;

        File[] sessions = logsDir.listFiles(file -> file.isDirectory() && !"latest".equalsIgnoreCase(file.getName()));
        if (sessions == null || sessions.length <= maxSessions) return;

        Arrays.sort(sessions, (left, right) -> Long.compare(right.lastModified(), left.lastModified()));
        for (int i = maxSessions; i < sessions.length; i++) {
            try {
                deleteRecursively(sessions[i]);
            } catch (IOException ignored) {}
        }
    }

    private void copyDirectory(File source, File target) throws IOException {
        if (!target.exists() && !target.mkdirs())
            throw new IOException("Could not create " + target.getPath());

        File[] files = source.listFiles();
        if (files == null) return;

        for (File file : files) {
            File destination = new File(target, file.getName());
            if (file.isDirectory()) {
                copyDirectory(file, destination);
            } else {
                Files.copy(file.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void deleteRecursively(File file) throws IOException {
        if (file == null || !file.exists()) return;

        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) deleteRecursively(child);
        }

        Files.deleteIfExists(file.toPath());
    }

    private void writeFile(File file, List<String> lines) throws IOException {
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
    }

    private List<String> stripAll(List<String> lines) {
        List<String> stripped = new ArrayList<>();
        if (lines == null) return stripped;

        for (String line : lines) stripped.add(strip(line));
        return stripped;
    }

    private String strip(String line) {
        return line == null ? "" : COLOR_PATTERN.matcher(line).replaceAll("");
    }

    private String repeat(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) builder.append('-');
        return builder.toString();
    }
}
