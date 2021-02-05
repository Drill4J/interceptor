/**
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress


class JvmTest : TestBase() {

    override fun setupServer() {
        val server = HttpServer.create()
        server.bind(InetSocketAddress(0), 0)
        server.createContext("/") { exchange ->
//            generateBigSizeHeaders(exchange)
            val bytes = responseMessage.toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            val os = exchange.responseBody
            os.write(bytes)
            os.close()
        }
        server.start()
        port = server.address.port
    }

    private fun generateBigSizeHeaders(exchange: HttpExchange) {
        repeat(30000) {
            exchange.responseHeaders["header$it"] = listOf("any")
        }
    }

}