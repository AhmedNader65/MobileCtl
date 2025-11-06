plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":shared"))
    implementation("com.github.ajalt.clikt:clikt:5.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-framework-engine:5.7.2")
    testImplementation("io.kotest:kotest-assertions-core:5.7.2")
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

    // Exclude signature files that cause conflicts
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/LICENSE*")
    exclude("META-INF/NOTICE*")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveFileName.set("mobilectl.jar")
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

// Create a distribution task
tasks.jar {
    doLast {
        val jarFile = File(archiveFile.get().asFile.absolutePath)
        jarFile.setExecutable(true)
    }
}

tasks.build {
    dependsOn(tasks.jar)
}