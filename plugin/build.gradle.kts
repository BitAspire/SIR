val coreProject = project(":core")
val coreMainOutput = coreProject.extensions.getByType<SourceSetContainer>()["main"].output

dependencies {
    implementation(coreProject)
    compileOnly(project(":module:emojis"))
    compileOnly(project(":module:tags"))
}

val moduleProjects = rootProject.subprojects.filter { it.path.startsWith(":module:") }
val moduleJarTasks = moduleProjects.map { it.tasks.named<Jar>("jar") }

val commandProjects = rootProject.subprojects.filter { it.path.startsWith(":command:") }
val commandJarTasks = commandProjects.map { it.tasks.named<Jar>("jar") }

tasks.named<Jar>("jar") {
    dependsOn(coreProject.tasks.named("classes"))
    from(coreMainOutput)

    dependsOn(moduleJarTasks)
    moduleJarTasks.forEach { jarTask ->
        from(jarTask.flatMap { it.archiveFile }) {
            into("modules")
        }
    }

    dependsOn(commandJarTasks)
    commandJarTasks.forEach { jarTask ->
        from(jarTask.flatMap { it.archiveFile }) {
            into("commands")
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

    dependsOn(commandJarTasks)
    commandJarTasks.forEach { jarTask ->
        from(jarTask.flatMap { it.archiveFile }) {
            into("commands")
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