plugins {
    id("java")
    id("com.github.johnrengelman.shadow")
}

group = "io.github.somehussar.janinoloader"
version = "1.0.1-ALPHA"

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
//    implementation("com.google.auto.service:auto-service:1.1.1")

    // Use local snapshot build with generic return type fix (3.1.13-SNAPSHOT)
    includedInJar(files("lib/janino-3.1.13-SNAPSHOT.jar"))
    includedInJar(files("lib/commons-compiler-3.1.13-SNAPSHOT.jar"))
    includedInJar(files("lib/commons-compiler-jdk-3.1.13-SNAPSHOT.jar"))
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
