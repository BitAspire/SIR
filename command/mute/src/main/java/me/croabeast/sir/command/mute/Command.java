package me.croabeast.sir.command.mute;

import me.croabeast.command.TabBuilder;
import me.croabeast.common.time.TimeFormatter;
import me.croabeast.common.time.TimeValues;
import me.croabeast.sir.command.SIRCommand;
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
        TimeValues values = section == null ? null : TimeValues.fromSection(section);
        return new TimeFormatter(main.getApi().getLibrary(), values, remaining).formatTime();
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        return createBasicTabBuilder().addArguments(0, getOnlineNames()).addArgument(1, "<reason>");
    }
}
