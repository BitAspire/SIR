package me.croabeast.sir.module.announcement;

import lombok.Getter;
import me.croabeast.file.Configurable;
import me.croabeast.sir.PermissibleUnit;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.SoundSection;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
final class Announcement implements PermissibleUnit {

    private final Announcements main;
    private final ConfigurationSection section;
    private SoundSection sound;
    private final List<String> worlds, lines, commands;

    Announcement(Announcements main, ConfigurationSection section) {
        this.main = main;
        this.section = section;

        try {
            SoundSection temp = new SoundSection(section.getConfigurationSection("sound"));
            if (temp.isEnabled()) sound = temp;
        } catch (Exception ignored) {}

        worlds = Configurable.toStringList(section, "worlds");
        lines = Configurable.toStringList(section, "lines");
        lines.replaceAll(SIRApi.instance().getLibrary().getCharacterManager()::align);
        commands = Configurable.toStringList(section, "commands");
    }

    Set<SIRUser> getUsers() {
        Set<SIRUser> users = new HashSet<>();

        for (Player p : Bukkit.getOnlinePlayers()) {
            String world = p.getWorld().getName();
            if (!worlds.isEmpty() && !worlds.contains(world))
                continue;

            SIRUser user = SIRApi.instance().getUserManager().getUser(p);
            if (user == null || !main.isToggled(user)) continue;

            if (!user.isVanished() && hasPermission(user))
                users.add(user);
        }

        return users;
    }

    void announce() {
        Set<SIRUser> users = getUsers();
        if (users.isEmpty()) return;

        SIRApi.instance().getLibrary()
                .getLoadedSender()
                .setTargets(users.stream().map(SIRUser::getPlayer).collect(Collectors.toList()))
                .send(lines);
        if (sound != null) users.forEach(u -> sound.playSound(u));
        SIRApi.executeCommands(null, commands);
    }
}
