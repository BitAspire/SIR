package com.bitaspire.sir.command.mute;

import me.croabeast.command.TabBuilder;
import me.croabeast.common.time.TimeFormatter;
import me.croabeast.common.time.TimeValues;
import com.bitaspire.sir.command.SIRCommand;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

abstract class Command extends SIRCommand {

    final MuteProvider main;

    Command(MuteProvider main, String name) {
        super(name, Objects.requireNonNull(main.lang, "Mute lang file not initialized"));
        this.main = main;
    }

    String parseTime(long remaining) {
        ConfigurationSection section = getLang().getSection("lang.time");
        return new TimeFormatter(main.getApi().getLibrary(), section == null ? null : TimeValues.fromSection(section), remaining).formatTime();
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        return Utils.newBuilder().addArguments(0, Utils.getOnlineNames()).addArgument(1, "<reason>");
    }
}
