dependencies {
    api(project(":api"))
}

tasks.named<Javadoc>("javadoc") {
    enabled = false
}
