import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    kotlin("jvm") version "2.3.0"
    id("java-library")
    id("io.freefair.lombok") version "9.4.0"
    id("com.gradleup.shadow") version "9.4.1"
    id("maven-publish")
}

allprojects {
    group = "com.bitaspire.sir"
    version = "2.1.0"

    repositories {
        mavenCentral()
        mavenLocal()

        flatDir { dirs(rootProject.file("libraries")) }

        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://jitpack.io")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://croabeast.github.io/repo/")
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "maven-publish")
    java {
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
        options.compilerArgs.add("-Xlint:-options")
    }

    tasks.withType<Javadoc>().configureEach {
        isFailOnError = false

        (options as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:none", "-quiet")
            encoding = "UTF-8"
            charSet = "UTF-8"
            docEncoding = "UTF-8"

            if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_1_9))
                addBooleanOption("html5", true)
        }
    }

    dependencies {
        compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")

        compileOnly("org.jetbrains:annotations:26.0.2")
        annotationProcessor("org.jetbrains:annotations:26.0.2")

        compileOnly("org.projectlombok:lombok:1.18.44")
        annotationProcessor("org.projectlombok:lombok:1.18.44")

        compileOnly("me.croabeast.takion:shaded:1.5.1:all")
        compileOnly("me.clip:placeholderapi:2.12.2")
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
            }
        }
    }
}

tasks.build {
    dependsOn("shadowJar")
}
