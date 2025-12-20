package me.croabeast.sir.module.moderation;

import lombok.Getter;
import me.croabeast.sir.ExtensionFile;

import java.util.Locale;

@Getter
final class Config {

    private String swearingName = "Swearing", capsName = "Caps", linksName = "Links", formatName = "Format";

    private boolean violationLogging = true;
    private String violationLogFormat = "<p> &7{player} incurs &e{type}&7 violation here: &f\"{message}\"";

    private boolean staffNotified = true;
    private String notifyPermission = "sir.moderation.staff.notify";

    Config(Moderation main) {
        try {
            ExtensionFile file = new ExtensionFile(main, "config", true);

            swearingName = file.get("lang-names.swearing", swearingName);
            capsName = file.get("lang-names.caps", capsName);
            formatName = file.get("lang-names.format", formatName);
            linksName = file.get("lang-names.links", linksName);

            violationLogging = file.get("log-violations.enabled", true);
            violationLogFormat = file.get("log-violations.format", violationLogFormat);

            staffNotified = file.get("notify-staff.enabled", true);
            notifyPermission = file.get("notify-staff.permission", notifyPermission);
        } catch (Exception ignored) {}
    }

    String getName(String name) {
        switch (name.toLowerCase(Locale.ENGLISH)) {
            case "swearing": return swearingName;
            case "caps": return capsName;
            case "format": return formatName;
            case "links": return linksName;
            default: return "";
        }
    }
}
