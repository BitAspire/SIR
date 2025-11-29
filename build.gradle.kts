plugins {
    kotlin("jvm") version "2.3.0-Beta1"
    id("java-library")
    id("io.freefair.lombok") version "8.10"
    id("com.gradleup.shadow") version "8.3.0"
}

allprojects {
    group = "me.croabeast"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        mavenLocal()

        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://croabeast.github.io/repo/")
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "com.gradleup.shadow")

    java {
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }

    dependencies {
        compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")

        compileOnly("org.jetbrains:annotations:26.0.2")
        annotationProcessor("org.jetbrains:annotations:26.0.2")

        compileOnly("org.projectlombok:lombok:1.18.38")
        annotationProcessor("org.projectlombok:lombok:1.18.38")

        compileOnly("com.zaxxer:HikariCP:3.4.5")
        compileOnly("me.clip:placeholderapi:2.11.6")

        compileOnly("com.github.stefvanschie.inventoryframework:IF:0.11.5")
        implementation("me.croabeast.takion:shaded-all:1.3")
    }
}

tasks.build {
    dependsOn("shadowJar")
}
