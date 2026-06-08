package com.bitaspire.sir;

import lombok.Getter;
import me.croabeast.takion.logger.LogLevel;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

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

public abstract class BaseStartupDiagnostics implements RuntimeDiagnostics {

    private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)&[0-9a-fk-or]|</?[^>]+>");

    private final JavaPlugin plugin;
    private final boolean saveDetails;
    private final boolean latestFolder;
    private final int maxSessions;
    private final String consoleMode;
    private final String sessionName;

    @Getter
    private final DiagnosticSection moduleSection = createSection("Modules");
    @Getter
    private final DiagnosticSection commandSection = createSection("Commands");
    private final List<String> integrationLines = new ArrayList<>();
    private final List<String> fullLines = new ArrayList<>();

    @Getter
    private boolean collecting = true;

    protected BaseStartupDiagnostics(@NotNull JavaPlugin plugin,
                                     boolean saveDetails,
                                     boolean latestFolder,
                                     int maxSessions,
                                     @NotNull String consoleMode) {
        this.plugin = plugin;
        this.saveDetails = saveDetails;
        this.latestFolder = latestFolder;
        this.maxSessions = Math.max(0, maxSessions);
        this.consoleMode = consoleMode;
        this.sessionName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ENGLISH).format(new Date());
    }

    @Override
    public boolean notLogToConsole(LogLevel level) {
        return !isVerboseConsole() && level != LogLevel.ERROR;
    }

    public boolean hasDetails() {
        return saveDetails;
    }

    public String getDetailPath() {
        if (!saveDetails) return null;
        return latestFolder ? "logs/latest/" : "logs/" + sessionName + "/";
    }

    @Override
    public void module(LogLevel level, String... messages) {
        record(moduleSection, level, messages);
    }

    @Override
    public void command(LogLevel level, String... messages) {
        record(commandSection, level, messages);
    }

    @Override
    public void moduleRequirement(String moduleName, String[] dependencies) {
        addRequirement(moduleSection, "Module", moduleName, dependencies);
    }

    @Override
    public void commandRequirement(String providerName, String[] dependencies) {
        addRequirement(commandSection, "Provider", providerName, dependencies);
    }

    protected final DiagnosticSection createSection(@NotNull String title) {
        return new DiagnosticSection(title);
    }

    protected final void record(@NotNull DiagnosticSection section, LogLevel level, String... messages) {
        if (messages == null) return;

        for (String message : messages) {
            if (message == null || message.trim().isEmpty()) continue;

            String line = "[" + level + "] " + strip(message);
            section.events.add(line);
            fullLines.add("[" + section.title + "] " + line);
        }
    }

    protected final void recordIntegration(String line) {
        if (line == null || line.trim().isEmpty()) return;

        String plain = strip(line);
        integrationLines.add(plain);
        fullLines.add("[Integrations] " + plain);
    }

    protected final void addRequirement(@NotNull DiagnosticSection section,
                                        @NotNull String type,
                                        String name,
                                        String[] dependencies) {
        if (name == null || dependencies == null || dependencies.length == 0) return;

        String line = type + " " + name + " needs " + joinDependencies(dependencies);
        if (!section.requirements.contains(line)) section.requirements.add(line);
    }

    protected final SectionOutput output(@NotNull String fileName,
                                         @NotNull DiagnosticSection section,
                                         List<String> snapshot) {
        return new SectionOutput(fileName, section, snapshot);
    }

    protected final void writeDiagnostics(List<String> summary,
                                          List<String> integrationSnapshot,
                                          List<String> json,
                                          SectionOutput... outputs) {
        collecting = false;
        if (!saveDetails) return;

        List<SectionOutput> sections = Arrays.asList(outputs);
        File logsDir = new File(plugin.getDataFolder(), "logs");
        File sessionDir = uniqueSessionDir(logsDir);

        try {
            if (!sessionDir.exists() && !sessionDir.mkdirs())
                throw new IOException("Could not create " + sessionDir.getPath());

            writeFile(new File(sessionDir, "summary.log"), stripAll(summary));
            for (SectionOutput section : sections) {
                writeFile(
                        new File(sessionDir, section.fileName),
                        renderSection(
                                section.section.title,
                                section.snapshot,
                                section.section.requirements,
                                section.section.events
                        )
                );
            }
            writeFile(new File(sessionDir, "integrations.log"), integrations(integrationSnapshot, sections));
            writeFile(new File(sessionDir, "startup-full.log"), full(summary, integrationSnapshot, sections));
            writeFile(new File(sessionDir, "diagnostics.json"), json);

            if (latestFolder) refreshLatest(logsDir, sessionDir);
            pruneSessions(logsDir);
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to write startup diagnostics: " + exception.getMessage());
        }
    }

    protected final void beginRuntimeDiagnostics() {
        clearRuntimeDiagnostics();
        collecting = true;
    }

    protected final String writeRuntimeDiagnostics(@NotNull String operation,
                                                   long durationMs,
                                                   SectionOutput... outputs) {
        collecting = false;
        if (!saveDetails) return null;

        String normalized = operation.trim().isEmpty()
                ? "runtime"
                : operation.trim().toLowerCase(Locale.ENGLISH);

        List<String> summary = new ArrayList<>();
        summary.add(capitalize(normalized));
        summary.add(repeat(normalized.length()));
        summary.add("Duration: " + durationMs + "ms");

        List<SectionOutput> sections = Arrays.asList(outputs);
        File logsDir = new File(plugin.getDataFolder(), "logs" + File.separator + normalized + "s");
        File sessionDir = uniqueSessionDir(logsDir);

        try {
            if (!sessionDir.exists() && !sessionDir.mkdirs())
                throw new IOException("Could not create " + sessionDir.getPath());

            writeFile(new File(sessionDir, "summary.log"), summary);
            for (SectionOutput section : sections) {
                writeFile(
                        new File(sessionDir, section.fileName),
                        renderSection(
                                section.section.title,
                                section.snapshot,
                                section.section.requirements,
                                section.section.events
                        )
                );
            }
            writeFile(new File(sessionDir, normalized + "-full.log"), runtimeFull(summary, sections));

            if (latestFolder) refreshLatest(logsDir, sessionDir);
            pruneSessions(logsDir);

            return latestFolder
                    ? "logs/" + normalized + "s/latest/"
                    : "logs/" + normalized + "s/" + sessionDir.getName() + "/";
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to write " + normalized + " diagnostics: " + exception.getMessage());
            return null;
        }
    }

    protected final void clearRuntimeDiagnostics() {
        clearSection(moduleSection);
        clearSection(commandSection);
        integrationLines.clear();
        fullLines.clear();
    }

    protected final void clearSection(@NotNull DiagnosticSection section) {
        section.events.clear();
        section.requirements.clear();
    }

    private boolean isVerboseConsole() {
        return "verbose".equalsIgnoreCase(consoleMode) || "debug".equalsIgnoreCase(consoleMode);
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

    private List<String> renderSection(String title,
                                       List<String> snapshot,
                                       List<String> requirements,
                                       List<String> events) {
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

    private List<String> integrations(List<String> snapshot, List<SectionOutput> sections) {
        List<String> lines = renderSection("Integrations", snapshot, new ArrayList<String>(), integrationLines);
        List<String> requirements = new ArrayList<>();

        for (SectionOutput section : sections)
            requirements.addAll(section.section.requirements);

        if (!requirements.isEmpty()) {
            lines.add("");
            lines.add("Requirements");
            lines.addAll(requirements);
        }

        return lines;
    }

    private List<String> full(List<String> summary,
                              List<String> integrationSnapshot,
                              List<SectionOutput> sections) {
        List<String> lines = new ArrayList<>();
        lines.add("Summary");
        lines.add("-------");
        lines.addAll(stripAll(summary));

        for (SectionOutput section : sections) {
            lines.add("");
            lines.addAll(renderSection(
                    section.section.title,
                    section.snapshot,
                    section.section.requirements,
                    section.section.events
            ));
        }

        lines.add("");
        lines.addAll(integrations(integrationSnapshot, sections));

        if (!fullLines.isEmpty()) {
            lines.add("");
            lines.add("Raw Events");
            lines.addAll(fullLines);
        }

        return lines;
    }

    private List<String> runtimeFull(List<String> summary, List<SectionOutput> sections) {
        List<String> lines = new ArrayList<>(summary);

        for (SectionOutput section : sections) {
            lines.add("");
            lines.addAll(renderSection(
                    section.section.title,
                    section.snapshot,
                    section.section.requirements,
                    section.section.events
            ));
        }

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

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) return "";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    protected static final class DiagnosticSection {

        private final String title;
        private final List<String> events = new ArrayList<>();
        private final List<String> requirements = new ArrayList<>();

        private DiagnosticSection(@NotNull String title) {
            this.title = title;
        }
    }

    protected static final class SectionOutput {

        private final String fileName;
        private final DiagnosticSection section;
        private final List<String> snapshot;

        private SectionOutput(@NotNull String fileName,
                              @NotNull DiagnosticSection section,
                              List<String> snapshot) {
            this.fileName = fileName;
            this.section = section;
            this.snapshot = snapshot;
        }
    }
}
