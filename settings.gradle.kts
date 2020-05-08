rootProject.name = "hook-interceptor"
include(":http")
include(":http:test")

include(":http2")
include(":http2:test-grpc")
include(":http2:test")


val scriptUrl: String by extra
apply(from = "$scriptUrl/maven-repo.settings.gradle.kts")

pluginManagement {
    val kotlinVersion: String by extra
    val drillGradlePluginVersion: String by extra
    plugins {
        kotlin("multiplatform") version kotlinVersion
        id("com.epam.drill.cross-compilation") version drillGradlePluginVersion
    }
}

