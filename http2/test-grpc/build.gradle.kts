import com.google.protobuf.gradle.*

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.google.protobuf") version "0.8.8"
    id("idea")
}

repositories {
    google()
    jcenter()
    mavenCentral()
    mavenLocal()
}

val grpcVersion = "+"

val projectAgentName = ":http2:test"
val projectAgent = project(projectAgentName)

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation(projectAgent)
    implementation("io.grpc:grpc-netty-shaded:${grpcVersion}")
    implementation("io.grpc:grpc-protobuf:${grpcVersion}")
    implementation("io.grpc:grpc-stub:${grpcVersion}")
    compileOnly("javax.annotation:javax.annotation-api:1.2")

    testImplementation("io.grpc:grpc-testing:$grpcVersion")
    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:2.28.2")
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:3.11.0" }

    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:+"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}

val libName = "http2Interceptor"

val JVM_TEST_TARGET_NAME = "jvmAgent"

val linkJVMTIAgentTaskName = "link${libName.capitalize()}DebugShared${JVM_TEST_TARGET_NAME.capitalize()}"



tasks.withType<Test> {
    dependsOn("$projectAgentName:$linkJVMTIAgentTaskName")
    doFirst {
        val message = projectAgent
            .tasks["$linkJVMTIAgentTaskName"]
            .outputs
            .files
            .first()
            .listFiles().first { sequenceOf("dylib", "so", "dll").contains(it.extension) }
        jvmArgs("-agentpath:$message")
    }
    testLogging.showStandardStreams = false
}