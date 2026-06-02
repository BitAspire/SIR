group = "com.bitaspire.sir"

val takionVersion: String by project

dependencies {
    api("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    api("org.jetbrains:annotations:26.0.2")
    api("me.croabeast.takion:shaded:$takionVersion:all")
    api("me.clip:placeholderapi:2.12.2")
    api("com.github.stefvanschie.inventoryframework:IF:0.12.0")
}
