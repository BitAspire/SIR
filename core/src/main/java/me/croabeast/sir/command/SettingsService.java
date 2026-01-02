package me.croabeast.sir.command;

import me.croabeast.sir.ChatToggleable;
import me.croabeast.sir.user.SIRUser;

public interface SettingsService {

    boolean isToggled(SIRUser user, ChatToggleable toggleable);

    void setToggle(SIRUser user, ChatToggleable toggleable, boolean enabled);
}
