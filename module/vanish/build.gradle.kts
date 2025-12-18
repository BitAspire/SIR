repositories {
    maven("https://repo.essentialsx.net/releases/")
    maven("https://jitpack.io")
    flatDir {
        dirs("libraries")
    }
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":module:join-quit"))

    compileOnly("net.essentialsx:EssentialsX:2.21.0")
    compileOnly("com.github.LeonMangler:SuperVanish:6.2.18-3")
    compileOnly(files("libraries/CMI.jar"))
}