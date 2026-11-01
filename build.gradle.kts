plugins {
    kotlin("jvm") version "2.4.20"
}

group = "dev.seraphicness"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.betonquest.org/betonquest/")
    maven("https://repo.extendedclip.com/releases/")
}

val shade: Configuration by configurations.creating
configurations.implementation { extendsFrom(shade) }

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.235-stable")
    compileOnly("org.betonquest.betonquest:api:3.2.0") { isTransitive = false }
    compileOnly("org.betonquest.betonquest:core:3.2.0") { isTransitive = false }
    compileOnly("org.betonquest.betonquest:lib:3.2.0") { isTransitive = false }
    compileOnly("me.clip:placeholderapi:2.11.6") { isTransitive = false }
    shade(kotlin("stdlib"))
    testImplementation(kotlin("test"))
    testImplementation("io.papermc.paper:paper-api:26.3.build.235-stable")
}

kotlin {
    jvmToolchain(25)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveFileName.set("LinkDialogues-${project.version}.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(shade.map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/versions/**", "module-info.class")
    }
}

tasks.test {
    useJUnitPlatform()
}
