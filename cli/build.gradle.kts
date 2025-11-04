plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":shared"))
    implementation("com.github.ajalt.clikt:clikt:5.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

application {
    mainClass.set("com.mobilectl.MainKt")
}

// Build fat JAR
tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.mobilectl.MainKt"
        attributes["Implementation-Title"] = "mobilectl"
        attributes["Implementation-Version"] = version
    }
    from(configurations.runtimeClasspath.get()
        .map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Create distribution scripts
distributions {
    main {
        contents {
            from("README.md")
            from("LICENSE")
        }
    }
}
