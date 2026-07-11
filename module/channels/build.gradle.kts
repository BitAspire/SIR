repositories {
    maven("https://nexus.scarsz.me/content/groups/public/")
}

dependencies {
    compileOnly(project(":api"))
    compileOnly("com.discordsrv:discordsrv:1.30.1")
}
