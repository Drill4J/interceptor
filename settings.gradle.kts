rootProject.name = "hook-interceptor"
include(":http")
include(":http:test")
pluginManagement {
    repositories {
        maven(url = "http://oss.jfrog.org/oss-release-local")
        gradlePluginPortal()
    }
}
