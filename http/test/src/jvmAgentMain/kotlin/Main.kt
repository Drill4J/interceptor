@file:Suppress("UNUSED_PARAMETER")

import com.epam.drill.jvmapi.gen.JNIEnv
import com.epam.drill.jvmapi.gen.jobject
import com.epam.drill.logger.LoggerConfig
import com.epam.drill.logger.logConfig
import kotlin.native.concurrent.freeze

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
        isTraceEnabled = true,
        isDebugEnabled = true,
        isInfoEnabled = true,
        isWarnEnabled = true
    ).freeze()
    configureHttpInterceptor()
    headersForInject.value = { injectedHeaders }.freeze()
    readHttpCallback.value = { _: ByteArray -> println("READ") }.freeze()
    writeHttpCallback.value = { _: ByteArray -> println("WRITE") }.freeze()
}
