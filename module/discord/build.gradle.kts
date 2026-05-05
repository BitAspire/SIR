repositories {
    maven("https://nexus.scarsz.me/content/groups/public/")
    maven("https://repo.essentialsx.net/releases/")
}

dependencies {
    compileOnly(project(":api"))

    compileOnly("net.essentialsx:EssentialsX:2.21.0") {
        exclude("*", "*")
    }

    compileOnly("com.discordsrv:discordsrv:1.30.1")
    compileOnly("net.essentialsx:EssentialsXDiscord:2.21.0") {
        exclude("*", "*")
    }
}
