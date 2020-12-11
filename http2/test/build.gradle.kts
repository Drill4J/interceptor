import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

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

val libName = "http2Interceptor"

val JVM_TEST_TARGET_NAME = "jvmAgent"


val loggerVersion: String by extra
val drillJvmApiVersion: String by extra

kotlin {
    currentTarget(JVM_TEST_TARGET_NAME) {
        binaries.apply { sharedLib(libName, setOf(DEBUG)) }.forEach {
            if (org.jetbrains.kotlin.konan.target.HostManager.hostIsMingw)
                it.linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock")
        }
    }
    jvm {
        compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("reflect"))
                implementation(kotlin("test-junit"))

            }
        }
    }
//    val ktor_version = "1.3.2"
//    var tcnative_classifier =""
//    var osName = System.getProperty("os.name").toLowerCase()
//
//    if (osName.contains("win")) {
//        tcnative_classifier = "windows-x86_64"
//    } else if (osName.contains("linux")) {
//        tcnative_classifier = "linux-x86_64"
//    } else if (osName.contains("mac")) {
//        tcnative_classifier = "osx-x86_64"
//    } else {
//        tcnative_classifier = ""
//    }
//    val jvmCommonSourceset = jvm.compilations["main"].defaultSourceSet
//    jvm("JettyServer") {
//        compilations["test"].defaultSourceSet {
//            dependsOn(jvmCommonSourceset)
//            val tcnative_version = "2.0.28.Final"
//            dependencies {
//                implementation("io.ktor:ktor-server-netty:$ktor_version")
//                implementation("io.ktor:ktor-html-builder:$ktor_version")
//                implementation("io.ktor:ktor-network-tls:$ktor_version")
//                implementation("io.ktor:ktor-network-tls-certificates:$ktor_version")
//                implementation("io.netty:netty-tcnative:$tcnative_version")
//                implementation("io.netty:netty-tcnative-boringssl-static:$tcnative_version")
//                implementation("io.netty:netty-tcnative-boringssl-static:$tcnative_version:$tcnative_classifier")
//                implementation("io.ktor:ktor-client-jetty:$ktor_version")
//                implementation("org.eclipse.jetty.http2:http2-client")
//
//            }
//        }
//    }

    sourceSets {

        val common = maybeCreate("${JVM_TEST_TARGET_NAME}Main")
        with(common) {
            dependencies {
                api(project(":http2"))
                implementation("com.epam.drill:jvmapi:$drillJvmApiVersion")
                implementation("com.epam.drill.logger:logger:$loggerVersion")
            }

        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.ExperimentalStdlibApi"
}
tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}
val linkJVMTIAgentTaskName = "link${libName.capitalize()}DebugShared${JVM_TEST_TARGET_NAME.capitalize()}"
tasks.withType<org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest> {
    dependsOn(tasks.getByPath(linkJVMTIAgentTaskName))
    testLogging.showStandardStreams = true
    attachJVMTIAgent()
}


fun JavaForkOptions.attachJVMTIAgent() {
    val targetFromPreset = (kotlin.targets[JVM_TEST_TARGET_NAME]) as KotlinNativeTarget
    jvmArgs(
        "-agentpath:${targetFromPreset
            .binaries
            .findSharedLib(libName, org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.DEBUG)!!
            .outputFile.toPath()}"
    )
}
