repositories {
    maven("https://repo.essentialsx.net/releases/")
}

dependencies {
    api(project(":api"))

    compileOnly("net.essentialsx:EssentialsX:2.21.0") {
        exclude("*", "*")
    }
    compileOnly("com.github.Zrips:CMI-API:9.8.6.4")
    compileOnly("com.github.DevLeoko:AdvancedBan:2.3.0") {
        exclude(group = "org.bstats")
    }
}

tasks.named<Javadoc>("javadoc") {
    enabled = false
}
