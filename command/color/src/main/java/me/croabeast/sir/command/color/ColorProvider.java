package me.croabeast.sir.command.color;

import lombok.Getter;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.sir.command.StandaloneProvider;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.Set;

public final class ColorProvider extends StandaloneProvider {

    @Getter
    private final Set<SIRCommand> commands = new HashSet<>();

    ConfigurableFile data;
    Expansion expansion;

    @Override
    public boolean register() {
        try {
            data = new ExtensionFile(this, "data", true);
        } catch (Exception ignored) {}
        try {
            commands.add(new Command(this));
        } catch (Exception ignored) {}

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
            (expansion = new Expansion(this)).register();
        return true;
    }

    @Override
    public boolean unregister() {
        if (expansion != null) expansion.unregister();
        return true;
    }
}
