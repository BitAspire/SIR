package com.bitaspire.sir;

import lombok.Getter;
import lombok.Setter;
import me.croabeast.takion.logger.LogLevel;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

abstract class BaseManager<I extends Information> {

    final SIRApi api;

    final Set<String>  startupSkipped     = new LinkedHashSet<>();
    final Set<String>  startupFailures    = new LinkedHashSet<>();
    @Getter
    final List<String> startupUpdatedJars = new ArrayList<>();

    boolean deferPluginDependants = false;
    @Setter
    boolean quietConsole = false;

    BaseManager(SIRApi api) {
        this.api = api;
    }

    // -- Logging --

    void log(LogLevel level, String... messages) {
        if (!quietConsole || level == LogLevel.ERROR)
            api.getLibrary().getLogger().log(level, messages);
    }

    // -- Startup stats --

    void clearStartupStats() {
        startupSkipped.clear();
        startupFailures.clear();
        startupUpdatedJars.clear();
    }

    int getStartupSkippedCount()         { return startupSkipped.size(); }
    int getStartupFailedCount()          { return startupFailures.size(); }

    // -- Hook message (PluginDependant) --

    String hookMessage(String name, String[] plugins) {
        String type = extensionType();
        type = Character.toUpperCase(type.charAt(0)) + type.substring(1);
        return "&7" + type + " '" + name + "' can't be loaded since " +
                ((Function<String[], String>) strings -> {
                    final int length = strings.length;
                    if (length == 1)
                        return strings[0] + " is";

                    StringBuilder br = new StringBuilder();
                    for (int i = 0; i < length; i++) {
                        br.append(strings[i]);
                        if (i < length - 1)
                            br.append(i == length - 2 ? " or " : ", ");
                    }
                    return br.append(" are").toString();
                }).apply(plugins) + "n't installed in the server.";
    }

    // -- Extension identity --

    abstract String extensionFolder(); // "modules", "addons", "commands"
    abstract String extensionType();   // "module",  "addon",  "command"

    // -- Candidate (parsed descriptor + jar) --

    static class Candidate<T extends Information> {
        final File jarFile;
        final T file;

        Candidate(File jarFile, T file) {
            this.jarFile = jarFile;
            this.file = file;
        }
    }

    // yml filename inside the extension jar: "module.yml", "addon.yml", "commands.yml"
    abstract String ymlFileName();

    // parse the descriptor from the yml loaded from the extension jar
    abstract I parseInformation(YamlConfiguration config);

    Candidate<I> createCandidate(File jarFile) {
        String yml = ymlFileName();
        String type = extensionType();
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry(yml);
            if (entry == null) {
                startupFailures.add(jarFile.getName());
                log(LogLevel.WARN, yml + " not found in " + jarFile.getName() + ", skipping.");
                return null;
            }

            I file;
            try (InputStream in = jar.getInputStream(entry);
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                file = parseInformation(YamlConfiguration.loadConfiguration(reader));
            }

            return new Candidate<>(jarFile, file);
        } catch (Exception e) {
            startupFailures.add(jarFile.getName());
            log(LogLevel.ERROR, "Failed to read " + yml + " from " + jarFile.getName());
            e.printStackTrace();
            return null;
        }
    }

    // -- Bundled JAR support --

    abstract void onBundledJarExtracted(File jar);

    List<String> findBundledJars() {
        List<String> results = new ArrayList<>();
        String folder = extensionFolder();
        try {
            URL location = api.getPlugin().getClass().getProtectionDomain().getCodeSource().getLocation();
            if (location == null) return results;

            File source = new File(location.toURI());
            if (source.isFile()) {
                try (JarFile jar = new JarFile(source)) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.isDirectory()) continue;
                        String name = entry.getName();
                        if (name.startsWith(folder + "/") && name.endsWith(".jar"))
                            results.add(name);
                    }
                }
            } else if (source.isDirectory()) {
                File resourceDir = new File(source, folder);
                File[] jars = resourceDir.listFiles((dir, name) -> name.endsWith(".jar"));
                if (jars != null)
                    for (File jar : jars)
                        results.add(folder + "/" + jar.getName());
            }
        } catch (Exception e) {
            log(LogLevel.WARN, "Failed to inspect bundled " + extensionType() + " jars.");
            e.printStackTrace();
        }
        return results;
    }

    void loadBundledJars(boolean saveDefaults) {
        List<String> bundled = findBundledJars();
        if (bundled.isEmpty()) return;

        String folder = extensionFolder();
        String type   = extensionType();
        boolean alwaysUpdate = api.getConfiguration().isAlwaysUpdateJars();

        File outputDir = new File(api.getPlugin().getDataFolder(), folder);
        if (saveDefaults && !outputDir.exists() && !outputDir.mkdirs()) {
            log(LogLevel.WARN, "Could not create default " + type + "s directory: " + outputDir.getPath());
            saveDefaults = false;
        }

        for (String resource : bundled) {
            String fileName = resource.substring((folder + "/").length());
            File target;
            try {
                if (saveDefaults) {
                    target = new File(outputDir, fileName);
                } else {
                    target = File.createTempFile("sir-" + type + "-", ".jar", api.getPlugin().getDataFolder());
                    target.deleteOnExit();
                }
            } catch (Exception e) {
                log(LogLevel.ERROR, "Failed to create temp file for bundled " + type + " '" + fileName + "'.");
                e.printStackTrace();
                continue;
            }

            boolean existed = target.exists();
            if (!existed || !saveDefaults || alwaysUpdate) {
                try (InputStream stream = api.getPlugin().getClass().getClassLoader().getResourceAsStream(resource)) {
                    if (stream == null) {
                        log(LogLevel.WARN, "Bundled " + type + " jar '" + fileName + "' could not be found.");
                        continue;
                    }
                    Files.copy(stream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    if (saveDefaults && existed) startupUpdatedJars.add(fileName);
                } catch (Exception e) {
                    log(LogLevel.ERROR, "Failed to copy bundled " + type + " jar '" + fileName + "'.");
                    e.printStackTrace();
                    continue;
                }
            }

            if (!saveDefaults) onBundledJarExtracted(target);
        }
    }
}
