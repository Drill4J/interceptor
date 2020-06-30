import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    maven(url = "https://dl.bintray.com/kotlin/kotlinx")
    maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
}

val presetName: String =
    when {
        Os.isFamily(Os.FAMILY_MAC) -> "macosX64"
        Os.isFamily(Os.FAMILY_UNIX) -> "linuxX64"
        Os.isFamily(Os.FAMILY_WINDOWS) -> "mingwX64"
        else -> throw RuntimeException("Target ${System.getProperty("os.name")} is not supported")
    }

fun org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension.currentTarget(
    name: String = presetName,
    config: KotlinNativeTarget.() -> Unit = {}
): KotlinNativeTarget {
    val createTarget =
        (presets.getByName(presetName) as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTestsPreset).createTarget(
            name
        )
    targets.add(createTarget)
    config(createTarget)
    return createTarget
}

val libName = "hook"

val JVM_TEST_TARGET_NAME = "jvmAgent"


kotlin {
    currentTarget(JVM_TEST_TARGET_NAME) {
        binaries.apply { sharedLib(libName, setOf(DEBUG)) }.forEach {
            if (org.jetbrains.kotlin.konan.target.HostManager.hostIsMingw)
                it.linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock")
        }
    }
    val jvm = jvm {
        compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect"))
                implementation(kotlin("test-junit"))
            }
        }
    }
    val jvmCommonSourceset = jvm.compilations["main"].defaultSourceSet
    jvm("SunServer") {
        compilations["test"].defaultSourceSet {
            dependsOn(jvmCommonSourceset)
        }
    }

    jvm("UndertowServer") {
        compilations["test"].defaultSourceSet {
            dependsOn(jvmCommonSourceset)
            dependencies {
                implementation("io.undertow:undertow-core:2.0.29.Final")
                implementation("io.undertow:undertow-servlet:2.0.29.Final")
            }
        }
    }
    jvm("JettyServer") {
        compilations["test"].defaultSourceSet {
            dependsOn(jvmCommonSourceset)
            dependencies {
                implementation("org.eclipse.jetty:jetty-server:9.4.26.+")
            }
        }
    }

    sourceSets {

        val common = maybeCreate("${JVM_TEST_TARGET_NAME}Main")
        with(common) {
            dependencies {
                implementation("com.epam.drill.hook:drill-hook:1.2.1")
                api(project(":http"))
                implementation("com.epam.drill:jvmapi-native:0.5.0")
                implementation("com.epam.drill.logger:logger:0.3.0")
            }

        }


        val jvmsTargets = targets.filterIsInstance<org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget>()
            .filter { it.name != "jvm" }
        jvmsTargets.forEach {
            it.compilations.forEach { knCompilation ->
                if (knCompilation.name == "test") {
                    knCompilation.defaultSourceSet {
                        dependencies {
                            implementation(kotlin("test-junit"))
                        }
                    }
                } else {
                    knCompilation.defaultSourceSet {
                        dependencies {
                            implementation(kotlin("stdlib"))
                            implementation(kotlin("reflect"))
                        }
                    }
                }

            }
        }

        named("commonMain") {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.ExperimentalStdlibApi"
}
val linkJVMTIAgentTaskName = "link${libName.capitalize()}DebugShared${JVM_TEST_TARGET_NAME.capitalize()}"

tasks.withType<KotlinJvmTest> {
    dependsOn(tasks.getByPath(linkJVMTIAgentTaskName))
    testLogging.showStandardStreams = true
    attachJVMTIAgent()
}

tasks.withType<JavaExec> {
    dependsOn(tasks.getByPath(linkJVMTIAgentTaskName))
    attachJVMTIAgent()
}

fun JavaForkOptions.attachJVMTIAgent() {
    val targetFromPreset = (kotlin.targets[JVM_TEST_TARGET_NAME]) as KotlinNativeTarget
    jvmArgs(
        "-agentpath:${targetFromPreset
            .binaries
            .findSharedLib(libName, NativeBuildType.DEBUG)!!
            .outputFile.toPath()}"
    )
}
