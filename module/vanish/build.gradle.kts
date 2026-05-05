repositories {
    maven("https://repo.essentialsx.net/releases/")
}

dependencies {
    compileOnly(project(":api"))

    compileOnly("net.essentialsx:EssentialsX:2.21.0")
    compileOnly("com.github.Zrips:CMI-API:9.8.6.4")
    compileOnly("com.github.LeonMangler:SuperVanish:6.2.18-3")
}
