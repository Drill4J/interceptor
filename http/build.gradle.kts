plugins {
    kotlin("multiplatform")
    id("com.epam.drill.cross-compilation")
    `maven-publish`
}

val hookVersion: String by extra

kotlin {

    crossCompilation {
        common {
            defaultSourceSet {
                dependsOn(sourceSets.named("commonMain").get())
                dependencies {
                    implementation("com.epam.drill.hook:drill-hook:$hookVersion")
                    implementation("com.epam.drill.logger:logger:0.3.0")
                }

            }
        }
    }

    macosX64()
    mingwX64()
    linuxX64()

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
            }
        }
    }
}


tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest> {
    testLogging.showStandardStreams = true
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes"
}
