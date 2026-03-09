package com.bitaspire.sir.module;

import com.bitaspire.sir.user.SIRUser;

public interface JoinQuitService {

    boolean isOnCooldown(SIRUser user, boolean join);

    void display(SIRUser user, boolean join);
}
