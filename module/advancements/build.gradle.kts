val advancementInfoVersion: String by project

dependencies {
    compileOnly(project(":api"))
    // Takion's all jar dropped it in 2.2.3; the plugin jar shades it instead
    compileOnly("me.croabeast:AdvancementInfo:$advancementInfoVersion")
}