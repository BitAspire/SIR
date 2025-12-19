package me.croabeast.sir;

import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import org.jetbrains.annotations.NotNull;

public interface Information {

    @NotNull
    String getName();

    @NotNull
    String getTitle();

    @NotNull
    String[] getDescription();

    @NotNull
    default Slot getSlot() {
        throw new UnsupportedOperationException("Slot not implemented yet");
    }
}
