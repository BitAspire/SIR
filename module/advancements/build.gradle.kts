plugins {
    id("java-library")
    id("io.freefair.lombok")
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":module"))
    compileOnly(project(":api"))
    compileOnly(project(":module:discord"))
}