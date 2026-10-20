dependencies {
    compileOnly(project(":api"))

    val takionVersion: String by project
    testImplementation(project(":api"))
    testImplementation("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    testImplementation("me.croabeast.takion:shaded:$takionVersion:all")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
