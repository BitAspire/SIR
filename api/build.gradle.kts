plugins {
    id("java-library")
    id("io.freefair.lombok")
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":command"))
    compileOnly(project(":module"))
}