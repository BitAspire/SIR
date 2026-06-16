import org.gradle.api.file.DuplicatesStrategy

val moduleProjects = listOf(
    project(":module:advancements"),
    project(":module:announcements"),
    project(":module:channels"),
    project(":module:cooldowns"),
    project(":module:discord"),
    project(":module:emojis"),
    project(":module:join-quit"),
    project(":module:login"),
    project(":module:mentions"),
    project(":module:moderation"),
    project(":module:motd"),
    project(":module:scoreboard"),
    project(":module:tags"),
    project(":module:vanish")
)

dependencies {
    moduleProjects.forEach { api(it) }
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    moduleProjects.forEach { moduleProject ->
        val moduleJar = moduleProject.tasks.named<Jar>("jar")
        dependsOn(moduleJar)
        from(moduleJar.flatMap { it.archiveFile }.map { zipTree(it) })
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
