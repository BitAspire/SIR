import org.gradle.api.file.DuplicatesStrategy

val commandProjects = listOf(
    project(":command:clear-chat"),
    project(":command:color"),
    project(":command:ignore"),
    project(":command:message"),
    project(":command:mute"),
    project(":command:nick"),
    project(":command:print"),
    project(":command:settings")
)

dependencies {
    commandProjects.forEach { api(it) }
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    commandProjects.forEach { commandProject ->
        val commandJar = commandProject.tasks.named<Jar>("jar")
        dependsOn(commandJar)
        from(commandJar.flatMap { it.archiveFile }.map { zipTree(it) })
    }
}

tasks.named<Jar>("sourcesJar") {
    enabled = false
}

tasks.named<Jar>("javadocJar") {
    enabled = false
}

tasks.named("shadowJar") {
    enabled = false
}
