import zio.*
import zio.test.{assertTrue, ZIOSpecDefault}

import zhttp.http.*
import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zhttp.service.{Server, ServerChannelFactory}
import zhttp.service.server.ServerChannelFactory

object ZhttpSpec extends ZIOSpecDefault {

  def spec =
    suite("Testing Launching of the Server")(
      test("Server should be able to start on an unoccupied port") {

        val dummyRoute = Http.collect[Request] { case req @ Method.POST -> !! / "echo" =>
          Response(status = Status.Ok, data = req.data)
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

        val action = for {
          port           <- ZIO.serviceWith[Server.Start](_.port)
          resp           <- Client.request(
                              s"http://localhost:$port/echo",
                              method = Method.POST,
                              content = HttpData.fromString("echo string"),
                            )
          responseString <- resp.bodyAsString
        } yield assertTrue(port > 0, responseString == "echo string")

        action.provideLayer(env)
      },
    )
}
