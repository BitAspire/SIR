package me.croabeast.sir.module.announcement;

import lombok.Getter;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.PermissibleUnit;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

@Getter
final class Data {

    private final Map<String, Announcement> announcements = new LinkedHashMap<>();

    Data(Announcements main) {
        try {
            PermissibleUnit.loadUnits(new ExtensionFile(main, "announcements", true)
                            .getSection("announcements"), Announcement::new)
                    .forEach(e -> announcements.put(e.getName(), e));
        } catch (Exception ignored) {}
    }

    Announcement fromIndex(int count) {
        return new LinkedList<>(announcements.values()).get(count);
    }
}
