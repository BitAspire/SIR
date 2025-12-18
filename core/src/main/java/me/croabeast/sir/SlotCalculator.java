package me.croabeast.sir;

import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SlotCalculator {

    private final int START_SLOT = 10, COLUMNS = 7, ROW_STRIDE = 9, MAX_ROWS = 4;

    public Slot toSlot(int index) {
        int row = index / COLUMNS;
        return row >= MAX_ROWS ? null : Slot.fromIndex(START_SLOT + (row * ROW_STRIDE) + (index % COLUMNS));
    }

    public int getUsedRows(int itemCount) {
        return itemCount <= 0 ? 0 : ((itemCount + COLUMNS - 1) / COLUMNS);
    }

    public Slot getMenuLastSlot(int itemCount) {
        int rows = getUsedRows(itemCount);
        return rows <= 0 ? null : Slot.fromIndex(((rows + 2) * 9) - 1);
    }

    public int getTotalRows(int itemCount) {
        return (Math.max(1, Math.min(MAX_ROWS, getUsedRows(itemCount))) + 2) * 9;
    }
}
