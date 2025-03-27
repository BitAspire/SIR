package me.croabeast.sir.plugin.manager;

import me.croabeast.sir.plugin.aspect.AspectKey;
import me.croabeast.sir.plugin.command.SIRCommand;
import me.croabeast.sir.plugin.module.Actionable;
import me.croabeast.sir.plugin.module.PlayerFormatter;
import me.croabeast.sir.plugin.module.SIRModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public interface ModuleManager extends BaseManager<SIRModule> {

    @Nullable
    <S extends SIRModule> S getModule(AspectKey key);

    @Nullable
    <T> PlayerFormatter<T> getFormatter(AspectKey key);

    @Nullable
    Actionable getActionable(AspectKey key);

    @SuppressWarnings("unchecked")
    default <P, T> T fromParent(AspectKey key, Function<P, T> function) {
        SIRModule module = getModule(key);
        if (module == null) return null;

        try {
            return function.apply((P) module);
        } catch (Exception e) {
            return null;
        }
    }

    @NotNull Set<SIRCommand> getCommands(AspectKey key);

    @NotNull
    Set<SIRCommand> getCommands();
}
