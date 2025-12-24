package me.croabeast.sir.module.announcement;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.PermissibleUnit;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

@Getter
final class Data {

    private final Map<String, Announcement> announcements = new LinkedHashMap<>();

    @SneakyThrows
    Data(Announcements main) {
        PermissibleUnit.loadUnits(new ExtensionFile(main, "announcements", true)
                        .getSection("announcements"), Announcement::new)
                .forEach(e -> announcements.put(e.getName(), e));
    }

    Announcement fromIndex(int count) {
        return new LinkedList<>(announcements.values()).get(count);
    }
}
