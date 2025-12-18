repositories {
    maven("https://repo.nickuc.com/maven-releases/")
    maven("https://repo.codemc.org/repository/maven-public/")
    flatDir {
        dirs("libraries")
    }
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":module:join-quit"))

    compileOnly("fr.xephi:authme:5.6.0-SNAPSHOT")
    compileOnly("com.nickuc.openlogin:openlogin-universal:1.3")
    compileOnly("com.nickuc.login:nlogin-api:10.0")

    compileOnly(files("libraries/NexAuth.jar"))
    compileOnly(files("libraries/UserLogin.jar"))
}