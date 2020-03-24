import bindings.Bindings

fun main() {
    Bindings.addReadHttpHook()
    val client = Client("localhost", 60315)
    try {
        client.greet("world")
    } finally {
        client.shutdown()
    }
}