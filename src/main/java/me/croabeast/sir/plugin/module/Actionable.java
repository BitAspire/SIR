package me.croabeast.sir.plugin.module;

import me.croabeast.common.util.ReplaceUtils;

@FunctionalInterface
public interface Actionable {

    void act(Object... objects);

    static boolean failsCheck(Object[] o, Class<?>... c) {
        if (!ReplaceUtils.isApplicable(o, c)) return true;

        for (int i = 0; i < o.length; i++)
            if (!c[i].isInstance(o[i])) return true;

        return false;
    }
}
