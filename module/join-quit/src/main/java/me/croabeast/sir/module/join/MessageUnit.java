package me.croabeast.sir.module.join;

import lombok.Getter;
import me.croabeast.file.Configurable;
import me.croabeast.sir.PermissibleUnit;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.module.discord.Discord;
import me.croabeast.sir.user.SIRUser;
import me.croabeast.takion.message.MessageSender;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Getter
final class MessageUnit implements PermissibleUnit {

    private final SIRApi api;

    private final ConfigurationSection section;
    private final Messages.Type type;

    private SoundSection soundSection = null;
    private SpawnSection spawnSection = null;

    private final int invulnerable;

    private final List<String> publicList, privateList, commands;

    MessageUnit(SIRApi api, ConfigurationSection section, Messages.Type type) {
        this.api = api;
        this.section = section;
        this.type = type;

        try {
            SoundSection temp = new SoundSection(section.getConfigurationSection("sound"));
            if (temp.enabled) soundSection = temp;
        } catch (Exception ignored) {}

        try {
            SpawnSection temp = new SpawnSection(section.getConfigurationSection("spawn"));
            if (temp.enabled) spawnSection = temp;
        } catch (Exception ignored) {}

        invulnerable = section.getInt("invulnerable");

        publicList = Configurable.toStringList(section, "public");
        privateList = Configurable.toStringList(section, "private");
        commands = Configurable.toStringList(section, "commands");
    }

    static <T> T non(T value) {
        return Objects.requireNonNull(value);
    }

    void teleport(SIRUser user) {
        if (spawnSection != null) spawnSection.teleport(user);
    }

    void execute(SIRUser user) {
        MessageSender sender = api.getLibrary().getLoadedSender();
        Player player = user.isOnline() ? user.getPlayer() : null;

        sender.copy().setParser(player).setTargets(Bukkit.getOnlinePlayers()).send(publicList);
        if (type != Messages.Type.QUIT) {
            sender.copy().setTargets(player).send(privateList);
            user.getImmuneData().giveImmunity(invulnerable);
            if (soundSection != null) soundSection.playSound(user);
            teleport(user);
        }

        SIRApi.executeCommands(type == Messages.Type.QUIT ? null : user, commands);
        if (!api.getModuleManager().isEnabled("Discord")) return;

        Discord discord = api.getModuleManager().getModule(Discord.class);
        if (discord != null)
            discord.sendMessage(type == Messages.Type.FIRST_JOIN ?
                    "first-join" :
                    type.name().toLowerCase(Locale.ENGLISH), player, s -> s);
    }

     private static class SoundSection {

        private final boolean enabled;
        private final Sound sound;
        private float volume = 1.0F, pitch = 1.0F;

        SoundSection(@Nullable ConfigurationSection section) {
            enabled = non(section).getBoolean("enabled");
            sound = Sound.valueOf(section.getString("type"));

            try {
                String temp = non(section.getString("volume"));
                volume = Float.parseFloat(temp);
            } catch (Exception ignored) {}

            try {
                String temp = non(section.getString("pitch"));
                pitch = Float.parseFloat(temp);
            } catch (Exception ignored) {}
        }

        void playSound(SIRUser user) {
            user.playSound(sound, volume, pitch);
        }
    }

    private static class SpawnSection {

        private final boolean enabled;
        private final Location location;

        SpawnSection(@Nullable ConfigurationSection section) {
            enabled = non(section).getBoolean("enabled");

            final World world = Bukkit.getWorld(non(section.getString("world")));
            location = non(world).getSpawnLocation();

            try {
                String[] c = non(section.getString("coordinates")).split(",", 3);
                try {
                    location.setX(Double.parseDouble(c[0]));
                } catch (Exception ignored) {}
                try {
                    location.setY(Double.parseDouble(c[1]));
                } catch (Exception ignored) {}
                try {
                    location.setZ(Double.parseDouble(c[2]));
                } catch (Exception ignored) {}
            } catch (Exception ignored) {}

            try {
                String[] r = non(section.getString("rotation")).split(",", 3);
                try {
                    location.setYaw(Float.parseFloat(r[0]));
                } catch (Exception ignored) {}
                try {
                    location.setPitch(Float.parseFloat(r[1]));
                } catch (Exception ignored) {}
            } catch (Exception ignored) {}
        }

        void teleport(SIRUser user) {
            if (user.isOnline()) user.getPlayer().teleport(location);
        }
    }
}
