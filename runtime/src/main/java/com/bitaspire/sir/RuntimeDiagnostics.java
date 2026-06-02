package com.bitaspire.sir;

import me.croabeast.takion.logger.LogLevel;

interface RuntimeDiagnostics {

    boolean isCollecting();

    boolean notLogToConsole(LogLevel level);

    void module(LogLevel level, String... messages);

    void command(LogLevel level, String... messages);

    void moduleRequirement(String moduleName, String[] dependencies);

    void commandRequirement(String providerName, String[] dependencies);
}
