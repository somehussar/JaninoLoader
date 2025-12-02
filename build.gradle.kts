plugins {
    id("java")
}

group = "io.github.somehussar.janinoloader"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.codehaus.janino:janino:3.1.12")
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    shouldRunAfter(tasks.test)
    useJUnitPlatform()
}

tasks.test {
    useJUnitPlatform {
        includeTags("unit")
        excludeTags("integration")
    }
}
tasks.named<Test>("integrationTest") {
    useJUnitPlatform {
        includeTags("unit")
        includeTags("integration")
    }
}

tasks.jar {
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.startsWith("org.codehaus.janino:janino") }
            .map { zipTree(it) }
    })
}