package me.croabeast.sir;

import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class SlotCalculator {

    public static final SlotCalculator CENTER_LAYOUT = new SlotCalculator(10, 7, 9, 4);
    public static final SlotCalculator EXTENSION_LAYOUT = new SlotCalculator(12, 5, 9, 4);

    private final int startSlot, columns, rowStride, maxRows;

    public Slot toSlot(int index) {
        int row = index / columns;
        return row >= maxRows ? null : Slot.fromIndex(startSlot + (row * rowStride) + (index % columns));
    }

    public int getUsedRows(int itemCount) {
        return itemCount <= 0 ? 0 : ((itemCount + columns - 1) / columns);
    }

    public Slot getMenuLastSlot(int itemCount) {
        int rows = getUsedRows(itemCount);
        return rows <= 0 ? null : Slot.fromIndex(((rows + 2) * 9) - 1);
    }

    public int getTotalRows(int itemCount) {
        return (Math.max(1, Math.min(maxRows, getUsedRows(itemCount))) + 2) * 9;
    }
}
