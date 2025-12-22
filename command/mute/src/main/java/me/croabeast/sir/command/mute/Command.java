package me.croabeast.sir.command.mute;

import lombok.Getter;
import me.croabeast.command.TabBuilder;
import me.croabeast.common.time.TimeFormatter;
import me.croabeast.common.time.TimeValues;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.command.SIRCommand;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

abstract class Command extends SIRCommand {

    final MuteProvider main;
    @Getter
    private final ConfigurableFile lang;

    Command(MuteProvider main, String name) {
        super(name);
        this.lang = Objects.requireNonNull((this.main = main).lang);
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
