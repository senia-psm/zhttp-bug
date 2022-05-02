import zio.*
import zio.stream.ZStream
import zio.test.{assertTrue, ZIOSpecDefault}
import zio.test.TestAspect.timeout

import zhttp.http.*
import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zhttp.service.{Server, ServerChannelFactory}
import zhttp.service.server.ServerChannelFactory

object ZhttpSpec extends ZIOSpecDefault {

  def spec =
    suite("Testing Launching of the Server")(
      test("Server should be able to start on an unoccupied port") {

        val dummyRoute = Http.collect[Request] { case req @ Method.POST -> !! / "echo" =>
          Response(
            status = Status.Ok,
            data = HttpData.fromStream(req.data.toByteBufStream.mapConcat(buf => Chunk.fromByteBuffer(buf.nioBuffer()))),
          )
        }

        val layer: ZLayer[ServerChannelFactory & EventLoopGroup, Nothing, Server.Start] = ZLayer.scoped {
          Server(dummyRoute).withBinding("localhost", 0).make.orDie
        }
        val env                                                                         = ZLayer
          .make[Server.Start & EventLoopGroup & ChannelFactory](
            layer,
            EventLoopGroup.auto(0),
            ServerChannelFactory.auto,
            ChannelFactory.auto,
          )

        val stream = ZStream.repeatZIOChunk(ZIO.sleep(10.millis).as(Chunk[String]("A"))).take(10)

        val action = for {
          port           <- ZIO.serviceWith[Server.Start](_.port)
          resp           <- Client.request(
                              s"http://localhost:$port/echo",
                              method = Method.POST,
                              content = HttpData.fromStream(stream),
                            )
          responseString <- resp.bodyAsString
        } yield assertTrue(port > 0, responseString == "AAAAAAAAAA")

        action.provideLayer(env)
      },
    ) @@ timeout(5.seconds)
}
