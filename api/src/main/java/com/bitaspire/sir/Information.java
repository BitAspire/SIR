package com.bitaspire.sir;

import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Metadata descriptor for SIR extensions, modules, and addons.
 *
 * <p> Provides display information used in logs and the in-game management GUI.
 */
public interface Information {

    /**
     * Returns the internal identifier name of this component.
     *
     * @return the name.
     */
    @NotNull
    String getName();

    /**
     * Returns the human-readable display title shown in the GUI.
     *
     * @return the title.
     */
    @NotNull
    String getTitle();

    /**
     * Returns the description lines shown in the GUI item lore.
     *
     * @return the description lines.
     */
    @NotNull
    String[] getDescription();

    /**
     * Returns the inventory slot position for this item in the GUI.
     *
     * @return the slot.
     * @throws UnsupportedOperationException if the implementation does not define a slot.
     */
    @NotNull
    default Slot getSlot() {
        throw new UnsupportedOperationException("Slot not implemented yet");
    }

    /**
     * Returns the required plugin dependencies declared by this component.
     *
     * @return list of required plugin names; empty if none.
     */
    @NotNull
    default List<String> getDepend() {
        return Collections.emptyList();
    }

    /**
     * Returns the optional (soft) plugin dependencies declared by this component.
     *
     * @return list of optional plugin names; empty if none.
     */
    @NotNull
    default List<String> getSoftDepend() {
        return Collections.emptyList();
    }
}
