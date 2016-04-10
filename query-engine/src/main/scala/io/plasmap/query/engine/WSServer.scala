package io.plasmap.query.engine

import java.util.concurrent.TimeoutException

import _root_.io.plasmap.geo.data.OsmStorageService
import _root_.io.plasmap.geo.mappings.{IndexingService, MappingService}
import akka.actor.ActorSystem
import akka.http.ServerSettings
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, UpgradeToWebsocket}
import akka.stream._
import akka.stream.scaladsl._

import scala.collection.immutable.Seq
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Extractor to detect websocket messages. This checks whether the header
  * is available, and whether it contains an actual upgrade message.
  */
object WSRequest {

  def unapply(req: HttpRequest): Option[HttpRequest] = {
    if (req.header[UpgradeToWebsocket].isDefined) {
      req.header[UpgradeToWebsocket] match {
        case Some(upgrade) => Some(req)
        case None => None
      }
    } else None
  }

}

/**
  * Simple websocket server using akka-http and akka-streams.
  *
  * Note that about 600 messages get queued up in the send buffer (on mac, 146988 is default socket buffer)
  */
object WSServer extends App {

  // required actorsystem and flow materializer
  implicit val system = ActorSystem("websockets")
  implicit val fm = ActorMaterializer()

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  val storage = OsmStorageService()
  val index = IndexingService()
  val mapping = MappingService()

  println(s"Initialised $storage, $index, $mapping")


  // setup the actors for the stats
  // router: will keep a list of connected actorpublisher, to inform them about new stats.
  // vmactor: will start sending messages to the router, which will pass them on to any
  // connected routee
  //val router: ActorRef = system.actorOf(Props[RouterActor], "router")
  //val vmactor: ActorRef = system.actorOf(Props(classOf[VMActor], router ,2 seconds, 20 milliseconds))

  // Bind to an HTTP port and handle incoming messages.
  // With the custom extractor we're always certain the header contains
  // the correct upgrade message.
  // We can pass in a socketoptions to tune the buffer behavior
  // e.g options =  List(Inet.SO.SendBufferSize(100))
  val binding = Http().bindAndHandleSync({

    case WSRequest(req@HttpRequest(GET, Uri.Path("/api/websocket"), headers: Seq[HttpHeader], requestEntity: RequestEntity, httpProtocol: HttpProtocol)) =>
      handleWith(req, Flows.query(Flows.toQuery(fm,ec))(fm,ec))
    case other: HttpRequest =>
      HttpResponse(400, entity = "Invalid websocket request")

  }, interface = "0.0.0.0", port = 9000)


  // binding is a future, we assume it's ready within a second or timeout
  try {
    Await.result(binding, 1 second)
  } catch {
    case exc: TimeoutException =>
      println("Server took too long to startup, shutting down")
      system.shutdown()
  }

  /**
    * Simple helper function, that connects a flow to a specific websocket upgrade request
    */
  def handleWith(req: HttpRequest, flow: Flow[Message, Message, Unit]) =
    req.header[UpgradeToWebsocket].get.handleMessages(flow)

}

