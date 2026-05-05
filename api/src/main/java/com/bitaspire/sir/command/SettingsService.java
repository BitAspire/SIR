package com.bitaspire.sir.command;

import com.bitaspire.sir.ChatToggleable;
import com.bitaspire.sir.user.SIRUser;

public interface SettingsService {

    boolean isToggled(SIRUser user, ChatToggleable toggleable);

    void setToggle(SIRUser user, ChatToggleable toggleable, boolean enabled);
}
