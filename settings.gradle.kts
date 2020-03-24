rootProject.name = "hook-interceptor"
include(":http")
include(":http:test")

include(":http2")
include(":http2:test-grpc")
include(":http2:test")


pluginManagement {
    repositories {
        maven(url = "http://oss.jfrog.org/oss-release-local")
        maven(url = "https://dl.bintray.com/kotlin/kotlinx")
        gradlePluginPortal()
        google()
        jcenter()
        maven( // The google mirror is less flaky than mavenCentral()
            url = "https://maven-central.storage-download.googleapis.com/repos/central/data/"
        )
    }
}
