package me.croabeast.sir.module;

import me.croabeast.sir.SIRApi;

import java.net.URL;
import java.net.URLClassLoader;

final class ModuleLoader extends URLClassLoader {

    SIRModule module;

    ModuleLoader(SIRApi api, URL url) {
        super(new URL[] {url}, api.getPlugin().getClass().getClassLoader());
    }
}
