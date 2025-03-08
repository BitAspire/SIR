package me.croabeast.sir.plugin.module;

import me.croabeast.sir.plugin.aspect.AspectButton;
import me.croabeast.sir.plugin.command.SIRCommand;
import org.jetbrains.annotations.NotNull;

public abstract class ModuleCommand extends SIRCommand {

    protected final SIRModule module;

    protected ModuleCommand(SIRModule module, String name) {
        super(name);
        this.module = module;
    }

    @Override
    public boolean isEnabled() {
        return module.isEnabled() && super.isEnabled();
    }

    @NotNull
    public AspectButton getButton() {
        return module.getButton();
    }

    @Override
    public String toString() {
        return "ModuleCommand{name='" + getName() + "', module=" + module + '}';
    }
}
