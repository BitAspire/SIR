package me.croabeast.sir.command.color;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.sir.command.StandaloneProvider;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.Set;

public final class ColorProvider extends StandaloneProvider {

    @Getter
    private final Set<SIRCommand> commands = new HashSet<>();

    Expansion expansion;

    @SneakyThrows
    public boolean register() {
        commands.add(new Command(this));

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
            (expansion = new Expansion()).register();
        return true;
    }

    @Override
    public boolean unregister() {
        if (expansion != null) expansion.unregister();
        return true;
    }
}
