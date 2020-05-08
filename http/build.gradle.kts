plugins {
    id("org.jetbrains.kotlin.multiplatform") version "1.3.72"
    id("com.epam.drill.cross-compilation") version "0.16.0"
    `maven-publish`
}

repositories {
    mavenLocal()
    maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
    mavenCentral()
    jcenter()
}

kotlin {

    crossCompilation {
        common {
            defaultSourceSet {
                dependsOn(sourceSets.named("commonMain").get())
            }
            defaultSourceSet {
                dependencies {
                    implementation("com.epam.drill.hook:drill-hook:1.2.2")
                    implementation("com.epam.drill.logger:logger:0.1.2")
                }
            }
            posix{

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

publishing {
    repositories {
        maven {
            url = uri("https://oss.jfrog.org/oss-release-local")
            credentials {
                username =
                    if (project.hasProperty("bintrayUser"))
                        project.property("bintrayUser").toString()
                    else System.getenv("BINTRAY_USER")
                password =
                    if (project.hasProperty("bintrayApiKey"))
                        project.property("bintrayApiKey").toString()
                    else System.getenv("BINTRAY_API_KEY")
            }
        }
    }
}