import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    kotlin("plugin.serialization") version "2.0.0"
}

group = "com.mobilectl"
version = "0.1.0"

kotlin {
    jvm {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.kotest:kotest-framework-engine:5.7.2")
                implementation("io.kotest:kotest-assertions-core:5.7.2")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("com.github.ajalt.clikt:clikt:5.0.3")
                implementation("org.yaml:snakeyaml:2.5")
                implementation("commons-io:commons-io:2.13.0")
                implementation("org.eclipse.jgit:org.eclipse.jgit:6.7.0.202309050840-r")
//                implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.1") // Might be useful later
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation("io.mockk:mockk:1.13.7")
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}