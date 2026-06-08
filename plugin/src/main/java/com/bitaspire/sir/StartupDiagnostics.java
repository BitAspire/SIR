package com.bitaspire.sir;

import com.bitaspire.sir.file.Config;

import java.util.Collections;
import java.util.List;

final class StartupDiagnostics extends BaseStartupDiagnostics {

    private StartupDiagnostics(SIRPlugin plugin, ConfigImpl config) {
        super(
                plugin,
                config.isStartupLogDetails(),
                config.isStartupLogLatestFolder(),
                config.getStartupLogMaxSessions(),
                config.getStartupLogConsole()
        );
    }

    static StartupDiagnostics create(SIRPlugin plugin) {
        Config config = plugin.getConfiguration();
        ConfigImpl impl = config instanceof ConfigImpl ? (ConfigImpl) config : new ConfigImpl(plugin);
        return new StartupDiagnostics(plugin, impl);
    }

    void integration(String line) {
        recordIntegration(line);
    }

    void beginReload() {
        beginRuntimeDiagnostics();
    }

    String writeReload(long durationMs) {
        return writeRuntimeDiagnostics(
                "reload",
                durationMs,
                output("modules.log", getModuleSection(), Collections.emptyList()),
                output("commands.log", getCommandSection(), Collections.emptyList())
        );
    }

    void write(
            List<String> summary,
            List<String> moduleSnapshot,
            List<String> commandSnapshot,
            List<String> integrationSnapshot,
            List<String> json
    ) {
        writeDiagnostics(
                summary,
                integrationSnapshot,
                json,
                output("modules.log", getModuleSection(), moduleSnapshot),
                output("commands.log", getCommandSection(), commandSnapshot)
        );
    }
}
