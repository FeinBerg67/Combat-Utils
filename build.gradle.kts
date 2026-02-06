plugins {
    id("fabric-loom") version "1.11-SNAPSHOT"
}

base {
    archivesName = project.property("archives_base_name") as String
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

repositories {
    maven("https://maven.meteordev.org/releases")
    maven("https://maven.meteordev.org/snapshots")
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")

    // Meteor Client
    modImplementation("meteordevelopment:meteor-client:0.5.9-SNAPSHOT")
}

tasks {
    processResources {
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to project.version))
        }
    }

    withType<JavaCompile> {
        options.release = 21
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
