package me.croabeast.sir.module.tag;

import lombok.Getter;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.PermissibleUnit;
import me.croabeast.sir.user.SIRUser;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Getter
final class Data {

    private final Map<String, Tag> tags = new LinkedHashMap<>();

    Data(Tags main) {
        try {
            PermissibleUnit.loadUnits(new ExtensionFile(main, "tags", true)
                            .getSection("tags"), Tag::new)
                    .forEach(e -> this.tags.put(e.getTag(), e));
        } catch (Exception ignored) {}
    }

    @NotNull
    List<Tag> fromGroup(SIRUser user, String group) {
        List<Tag> tags = new ArrayList<>();
        if (user == null || !user.isOnline()) return tags;

        for (Tag tag : this.tags.values()) {
            if (!Objects.equals(tag.getGroup(), group))
                continue;

            if (tag.isInGroupNonNull(user.getPlayer()))
                tags.add(tag);
        }

        return tags;
    }

    Tag getTag(SIRUser user) {
        return PermissibleUnit.getUnit(user, tags.values(), true);
    }
}
