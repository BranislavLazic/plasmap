package io.plasmap.query.engine

import java.util.concurrent.TimeoutException

import _root_.io.plasmap.geo.data.OsmStorageService
import _root_.io.plasmap.geo.mappings.{IndexingService, MappingService}
import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, UpgradeToWebSocket}
import akka.stream._
import akka.stream.scaladsl._
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Extractor to detect websocket messages. This checks whether the header
  * is available, and whether it contains an actual upgrade message.
  */
object WSRequest {

  def unapply(req: HttpRequest): Option[HttpRequest] = {
    if (req.header[UpgradeToWebSocket].isDefined) {
      req.header[UpgradeToWebSocket] match {
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

  val log = Logger(LoggerFactory.getLogger(WSServer.getClass.getName))

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  val storage = OsmStorageService()
  val index = IndexingService()
  val mapping = MappingService()

  log.info(s"Initialised $storage, $index, $mapping")

  val binding = Http().bindAndHandleSync(requestHandler(), interface = "0.0.0.0", port = 9000)

  def requestHandler(flow : Flow[Message, Message, NotUsed] = Flows(fm,ec)): HttpRequest ⇒ HttpResponse = {
    case req @ HttpRequest(GET, Uri.Path("/api/websocket"), _, _, _) ⇒
      req.header[UpgradeToWebSocket] match {
        case Some(upgrade) ⇒ upgrade.handleMessages(flow)
        case None          ⇒ HttpResponse(400, entity = "Not a valid websocket request!")
      }
    case _: HttpRequest ⇒ HttpResponse(404, entity = "Unknown resource!")
  }

  // binding is a future, we assume it's ready within a second or timeout
  try {
    Await.result(binding, 1 second)
  } catch {
    case exc: TimeoutException =>
      println("Server took too long to startup, shutting down")
      system.terminate()
  }

}

