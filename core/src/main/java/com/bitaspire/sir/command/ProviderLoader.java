package com.bitaspire.sir.command;

import com.bitaspire.sir.SIRApi;

import java.net.URL;
import java.net.URLClassLoader;

final class ProviderLoader extends URLClassLoader {

    ProviderLoader(SIRApi api, URL url) {
        super(new URL[] {url}, api.getPlugin().getClass().getClassLoader());
    }

    @Override
    public URL getResource(String name) {
        URL resource = findResource(name);
        return resource != null ? resource : super.getResource(name);
    }
}
