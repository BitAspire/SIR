package com.bitaspire.sir.command.color;

import lombok.Getter;
import lombok.SneakyThrows;
import com.bitaspire.sir.PluginDependant;
import com.bitaspire.sir.command.SIRCommand;
import com.bitaspire.sir.command.StandaloneProvider;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public final class ColorProvider extends StandaloneProvider implements PluginDependant {

    private static final String PAPI = "PlaceholderAPI";

    @Getter
    private final Set<SIRCommand> commands = new HashSet<>();

    Expansion expansion;

    @NotNull
    public String[] getSoftDependencies() {
        return new String[]{PAPI};
    }

    @SneakyThrows
    public boolean register() {
        commands.add(new Command(this));

        if (isPluginEnabled(PAPI))
            (expansion = new Expansion()).register();
        return true;
    }

    @Override
    public boolean unregister() {
        if (expansion != null) expansion.unregister();
        return true;
    }
}
