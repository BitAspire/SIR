repositories {
    maven("https://repo.essentialsx.net/releases/")
    flatDir {
        dirs("libraries")
    }
}

val coreProject = project(":core")
val takionShaded: Configuration by configurations.creating

dependencies {
    implementation(coreProject)
    compileOnly(project(":module:emojis"))
    compileOnly(project(":module:tags"))

    compileOnly("net.essentialsx:EssentialsX:2.21.0") {
        exclude("*", "*")
    }
    compileOnly("com.github.DevLeoko:AdvancedBan:2.3.0")
    compileOnly(files("libraries/CMI.jar"))
    takionShaded("me.croabeast.takion:shaded-all:1.3")
    implementation("org.bstats:bstats-bukkit:3.0.2")
}

val coreMainOutput = coreProject.extensions.getByType<SourceSetContainer>()["main"].output

val moduleJarTasks = rootProject.subprojects.filter { it.path.startsWith(":module:") }.map { it.tasks.named<Jar>("jar") }
val commandJarTasks = rootProject.subprojects.filter { it.path.startsWith(":command:") }.map { it.tasks.named<Jar>("jar") }

tasks.named<Jar>("jar") {
    dependsOn(coreProject.tasks.named("classes"))
    from(coreMainOutput)
    from(takionShaded.files.map { if (it.isDirectory) it else zipTree(it) })

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

    archiveBaseName.set("SIR")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}