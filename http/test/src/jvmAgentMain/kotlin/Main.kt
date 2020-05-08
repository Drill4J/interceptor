@file:Suppress("UNUSED_PARAMETER", "FunctionName", "unused")

import com.epam.drill.jvmapi.gen.*
import com.epam.drill.jvmapi.gen.jobject
import com.epam.drill.logger.LoggerConfig
import com.epam.drill.logger.logConfig
import kotlin.native.concurrent.freeze
import com.epam.drill.interceptor.*
import com.epam.drill.jvmapi.JNIEnvPointer
import com.epam.drill.jvmapi.vmGlobal
import kotlinx.cinterop.CPointer

@Suppress("UNUSED_PARAMETER", "UNUSED")
@CName("Agent_OnLoad")
fun agentOnLoad(vmPointer: Long, options: String, reservedPtr: Long): Int {
    return 0
}

@Suppress("unused")
@CName("Java_bindings_Bindings_removeHttpHook")
fun removeHttpHook(env: JNIEnv, thiz: jobject) {
    com.epam.drill.hook.io.removeTcpHook()
}

@Suppress("unused")
@CName("Java_bindings_Bindings_addHttpHook")
fun addHttpHook(env: JNIEnv, thiz: jobject) {
    logConfig.value = LoggerConfig(
        isTraceEnabled = false,
        isDebugEnabled = false,
        isInfoEnabled = false,
        isWarnEnabled = false
    ).freeze()
    configureHttpInterceptor()
    com.epam.drill.hook.io.tcp.injectedHeaders.value = { injectedHeaders }.freeze()
    com.epam.drill.hook.io.tcp.readHeaders.value = { it: Map<ByteArray, ByteArray> ->
        it.forEach { (k, v) ->
//            println("${k.decodeToString()}: ${v.decodeToString()}")
        }
    }.freeze()
    com.epam.drill.hook.io.tcp.readCallback.value = { _: ByteArray -> }.freeze()
    com.epam.drill.hook.io.tcp.writeCallback.value = { _: ByteArray -> }.freeze()
}


@CName("currentEnvs")
fun currentEnvs(): JNIEnvPointer {
    return com.epam.drill.jvmapi.currentEnvs()
}

@CName("jvmtii")
fun jvmtii(): CPointer<jvmtiEnvVar>? {
    return com.epam.drill.jvmapi.jvmtii()
}

@CName("getJvm")
fun getJvm(): CPointer<JavaVMVar>? {
    return vmGlobal.value
}

@CName("JNI_OnUnload")
fun JNI_OnUnload() {
}

@CName("JNI_GetCreatedJavaVMs")
fun JNI_GetCreatedJavaVMs() {
}

@CName("JNI_CreateJavaVM")
fun JNI_CreateJavaVM() {
}

@CName("JNI_GetDefaultJavaVMInitArgs")
fun JNI_GetDefaultJavaVMInitArgs() {
}

@CName("checkEx")
fun checkEx(errCode: jvmtiError, funName: String): jvmtiError {
    return com.epam.drill.jvmapi.checkEx(errCode, funName)
}
