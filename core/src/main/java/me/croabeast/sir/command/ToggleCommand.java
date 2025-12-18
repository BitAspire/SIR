package me.croabeast.sir.command;

import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import lombok.Getter;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.Information;
import me.croabeast.sir.Toggleable;

@Getter
public abstract class ToggleCommand extends SIRCommand implements Toggleable {

    private final Button button;
    private final Slot slot;

    protected ToggleCommand(ConfigurableFile config, int x, int y) {
        super(config);
        slot = Slot.fromXY(x, y);

        button = new Button((Information) this, this, file.isEnabled());
        button.setDefaultItems();

        button.setOnClick(b -> e -> {
            config.set("enabled", b.isEnabled());
            config.save();

            reloadOptions();
            lib.getLogger().log("Command '/" + getName() + "' registering: " + b.isEnabled());
            b.toggleRegistering();
        });
    }
}
