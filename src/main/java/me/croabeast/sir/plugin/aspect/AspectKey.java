package me.croabeast.sir.plugin.aspect;

import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import me.croabeast.sir.api.BaseKey;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.NotNull;

public interface AspectKey extends BaseKey {

    @NotNull
    String[] getDescription();

    @NotNull
    Slot getMenuSlot();

    @NotNull
    default String getTitle() {
        return WordUtils.capitalize(getName().replaceAll("[_-]", " & "));
    }

    boolean isEnabled();
}
