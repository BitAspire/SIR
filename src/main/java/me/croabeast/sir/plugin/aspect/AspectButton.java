package me.croabeast.sir.plugin.aspect;

import com.github.stefvanschie.inventoryframework.pane.component.ToggleButton;
import lombok.Getter;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@Getter
public final class AspectButton extends ToggleButton {

    private final SIRAspect parent;

    public AspectButton(SIRAspect parent, boolean enabled) {
        super(parent.getAspectKey().getMenuSlot(), 1, 1, Priority.HIGHEST, enabled);
        this.parent = parent;
    }

    @NotNull
    public Consumer<InventoryClickEvent> getAction() {
        return Objects.requireNonNull(onClick);
    }

    @Override
    public void setOnClick(Consumer<InventoryClickEvent> consumer) {
        super.setOnClick(Objects.requireNonNull(consumer));
    }

    public void setOnClick(Function<AspectButton, Consumer<InventoryClickEvent>> function) {
        setOnClick(Objects.requireNonNull(function).apply(this));
    }
}
