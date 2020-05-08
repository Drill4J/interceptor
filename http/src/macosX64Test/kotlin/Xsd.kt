import com.epam.drill.interceptor.HTTP_VERBS
import com.epam.drill.interceptor.indexOf
import kotlin.system.measureTimeMillis
import kotlin.test.Test

class Xsd {

    val HTTP_VERBS_BYTES = HTTP_VERBS.map { it.encodeToByteArray() }

    @Test
    fun x() {
        val bytes = "POST       j".encodeToByteArray()
        println("xxx")
        val times = 10000000

        val byteArrayOf = byteArrayOf(
            bytes[0],
            bytes[1],
            bytes[2],
            bytes[3],
            bytes[4],
            bytes[5],
            bytes[6],
            bytes[7],
            bytes[8]
        )
        //warmup
        measureTimeMillis {
            repeat(times) {
                string(byteArrayOf)
            }
        }
        println(
            measureTimeMillis {
                repeat(times) {
                    string(byteArrayOf)
                }
            }
        )
        println(
            measureTimeMillis {
                repeat(times) {
                    bytes(byteArrayOf)
                }
            }
        )

        println(
            measureTimeMillis {
                repeat(times) {
                    directBytes(byteArrayOf)
                }
            }
        )
    }

    private fun directBytes(bytes: ByteArray) {

        HTTP_VERBS_BYTES.any { b ->
            (b.indices).all {
                bytes[it] == b[it]
            }
        }
    }

    private fun bytes(bytes: ByteArray) {
        HTTP_VERBS_BYTES.any {
            bytes.indexOf(it) != -1
        }
    }

    private fun string(bytes: ByteArray) {
        HTTP_VERBS.any {
            bytes.decodeToString().startsWith(it)
        }
    }
}