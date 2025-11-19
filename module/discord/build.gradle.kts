plugins {
    id("java-library")
    id("io.freefair.lombok")
}

repositories {
    maven("https://nexus.scarsz.me/content/groups/public/")
    maven("https://repo.essentialsx.net/releases/")
    flatDir {
        dirs("libraries")
    }
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":api"))
    compileOnly(project(":module"))

    compileOnly("net.essentialsx:EssentialsX:2.21.0") {
        exclude("*", "*")
    }

    compileOnly("com.discordsrv:discordsrv:1.30.1")
    compileOnly(files("libraries/EssentialsDiscord.jar"))
}