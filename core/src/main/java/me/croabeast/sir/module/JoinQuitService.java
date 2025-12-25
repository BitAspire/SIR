package me.croabeast.sir.module;

import me.croabeast.sir.user.SIRUser;

public interface JoinQuitService {

    boolean isOnCooldown(SIRUser user, boolean join);

    void display(SIRUser user, boolean join);
}
