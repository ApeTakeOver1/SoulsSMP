plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/maven-public/") // ProtocolLib
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    // Used for Echo Soul's fake-player afterimage and true (armor-hiding) invisibility packets.
    // Server needs the actual ProtocolLib.jar installed in /plugins - this is compileOnly,
    // not shaded in, since ProtocolLib should stay a separate plugin on the server.
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21.11")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version )
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}