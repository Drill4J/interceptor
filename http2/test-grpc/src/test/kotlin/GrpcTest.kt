import bindings.Bindings
import io.grpc.*
import io.grpc.examples.helloworld.GreeterGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.stub.StreamObserver
import org.junit.*
import java.util.concurrent.TimeUnit

class GrpcTest {

    private lateinit var server: Server

    internal class GreeterImpl : GreeterGrpc.GreeterImplBase() {

        override fun sayHello(req: HelloRequest, responseObserver: StreamObserver<HelloReply>) {
            val reply = HelloReply.newBuilder().setMessage("Hello ${req.name}").build()
            responseObserver.onNext(reply)
            responseObserver.onCompleted()
        }
    }

    @Before
    fun setup() {
        server = ServerBuilder.forPort(0).addService(GreeterImpl()).build().start()
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                server.shutdown()
            }
        })
    }

    @Test
    fun shouldSendGrpcRequest() {
        Bindings.addHttpHook()
//        server.awaitTermination()
        repeat(1) {
            ls()
        }

    }

    private fun ls() {
        val client = Client("localhost", server.port)
        try {
            client.greet("world")
        } finally {
            client.shutdown()
        }
    }
}

class Client(
    host: String,
    port: Int,
    private val channel: ManagedChannel = ManagedChannelBuilder.forAddress(host, port)
        .usePlaintext()
        .build()
) {
    private val blockingStub: GreeterGrpc.GreeterBlockingStub = GreeterGrpc.newBlockingStub(channel)

    fun shutdown() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

    fun greet(name: String) {
        val request = HelloRequest.newBuilder().setName(name).build()
        val response: HelloReply = try {
            blockingStub.sayHello(request)
        } catch (e: StatusRuntimeException) {
            return
        }
        println(response)

    }


}