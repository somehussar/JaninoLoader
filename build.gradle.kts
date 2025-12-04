plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "io.github.somehussar.janinoloader"
version = "0.0.0"

repositories {
    mavenCentral()
}


val includedInJar by configurations.creating { // Inherit from 'implementation'
    isCanBeConsumed = false
    isCanBeResolved = true
}

configurations {
    implementation {
        extendsFrom(includedInJar) // Make 'implementation' include dependencies from 'shadowed'
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Use the custom 'shadowed' configuration for shadow JAR dependencies
    includedInJar("org.codehaus.janino:janino:3.1.12")
    includedInJar("org.codehaus.janino:commons-compiler:3.1.12")
    includedInJar("org.codehaus.janino:commons-compiler-jdk:3.1.12")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveBaseName.set("standalone-${project.name}") // Set the name of the JAR file
    archiveVersion.set("${project.version}") // Optional version

    from(includedInJar) {

    }

}
tasks.build {
    dependsOn(tasks.shadowJar)
}