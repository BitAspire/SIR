val coreProject = project(":core")
val coreMainOutput = coreProject.extensions.getByType<SourceSetContainer>()["main"].output

dependencies {
    implementation(coreProject)
}

val moduleProjects = rootProject.subprojects.filter { it.path.startsWith(":module:") }
val moduleJarTasks = moduleProjects.map { it.tasks.named<Jar>("jar") }

tasks.named<Jar>("jar") {
    dependsOn(coreProject.tasks.named("classes"))
    from(coreMainOutput)
    dependsOn(moduleJarTasks)
    moduleJarTasks.forEach { jarTask ->
        from(jarTask.flatMap { it.archiveFile }) {
            into("modules")
        }
    }
}

tasks.named<Jar>("shadowJar") {
    dependsOn(coreProject.tasks.named("classes"))
    from(coreMainOutput)
    dependsOn(moduleJarTasks)
    moduleJarTasks.forEach { jarTask ->
        from(jarTask.flatMap { it.archiveFile }) {
            into("modules")
        }
    }
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}