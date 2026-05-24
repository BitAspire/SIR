import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.file.DuplicatesStrategy

repositories {
    maven("https://repo.essentialsx.net/releases/")
}

val apiProject = project(":api")
val takionShaded: Configuration by configurations.creating

dependencies {
    implementation(apiProject)

    compileOnly("net.essentialsx:EssentialsX:2.21.0") {
        exclude("*", "*")
    }
    compileOnly("com.github.Zrips:CMI-API:9.8.6.4")
    compileOnly("com.github.DevLeoko:AdvancedBan:2.3.0") {
        exclude(group = "org.bstats")
    }

    takionShaded("me.croabeast.takion:shaded:1.6.1:all")
}

val apiMainOutput = apiProject.extensions.getByType<SourceSetContainer>()["main"].output

val moduleJarTasks = rootProject.subprojects
    .filter { it.path.startsWith(":module:") && it.path != ":module:all" }
    .map { it.tasks.named<Jar>("jar") }

val commandJarTasks = rootProject.subprojects
    .filter { it.path.startsWith(":command:") && it.path != ":command:all" }
    .map { it.tasks.named<Jar>("jar") }

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("SIR")
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    dependsOn(apiProject.tasks.named("classes"))
    from(apiMainOutput)

    configurations = listOf(takionShaded)

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

    doFirst {
        val nestedJars = (moduleJarTasks + commandJarTasks).map { it.get().archiveFile.get().asFile }
        val shadowedNestedJars = nestedJars.filter { it.name.endsWith("-all.jar") }

        check(shadowedNestedJars.isEmpty()) {
            "Plugin modules and command providers must use regular jars, not shadow jars: " +
                shadowedNestedJars.joinToString { it.name }
        }
    }
}

tasks.assemble {
    dependsOn(tasks.named("shadowJar"))
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
