package io.plasmap.query.engine

import _root_.io.plasmap.model.geometry.Feature
import _root_.io.plasmap.query.engine.TypeAliases.SourceGraph
import _root_.io.plasmap.queryengine.macros.Macros.gottaMatchEmAll
import _root_.io.plasmap.serializer.{GeoJsonSerialiser, OsmDenormalizedSerializer}
import akka.http.scaladsl.model.ws.TextMessage.Strict
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.FanOutShape.Init
import com.typesafe.scalalogging.Logger
import io.plasmap.query.engine.QueryTranslator.{NotYetImplemented, TranslationError}
import io.plasmap.querymodel.PMSerialiser._
import akka.stream._
import akka.stream.scaladsl._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.language.experimental.macros
import scalaz.{-\/, \/, \/-}



/**
 * Contains methods to trenslate Websocket requests into Akka Streams Flows
  *
  * @author Jan Schulte <jan@plasmap.io>
 */
object Flows {
  val log = Logger(LoggerFactory.getLogger(QueryTranslator.getClass.getName))

  case class WSTerminatorShape[A](_init: Init[A] = akka.stream.FanOutShape.Name[A]("WSTerminator"))
    extends FanOutShape[A](_init) {
    val out = newOutlet[A]("nodes")

    protected override def construct(i: Init[A]) = WSTerminatorShape[A](i)
  }

  class WSTerminator[A] extends FlexiRoute[A, WSTerminatorShape[A]](
    WSTerminatorShape[A](), Attributes.name("WSTerminator")) {

    import FlexiRoute._

    override def createRouteLogic(p: PortT) = new RouteLogic[A] {
      override def initialCompletionHandling = CompletionHandling(
        onUpstreamFinish = (ctx) => {
//          case object EverythingIsAbsolutelyFineException extends Exception
//          ctx.fail(EverythingIsAbsolutelyFineException)
          ctx.finish()
        },
        onUpstreamFailure = (ctx, thr) => {
          ctx.finish()
        },
        onDownstreamFinish = (ctx, output) => {
          SameState
        }
      )

      override def initialState =
        State[Any](DemandFromAll(p.out)) {
          (ctx, _, elem) =>
            ctx.emit(p.out)(elem)
            SameState
        }
    }
  }

  object WSTerminator {
    def apply[A] = new WSTerminator[A]
  }

  sealed trait QueryError
  final case class UnpickleError() extends QueryError
  final case class QueryTranslationError(e: TranslationError) extends QueryError

  def translate(msg: String)(implicit mat: Materializer, ec: ExecutionContext): Source[String, Unit] = {
    val translatedQuery: QueryError \/ Query[_ <: Shape, _] = for {
      query           <- deserialiseQuery(msg).leftMap(_ => UnpickleError())
      translatedQuery <- QueryTranslator.translate(query)(mat, ec).leftMap(QueryTranslationError)
    } yield translatedQuery

    def poiMapImport(pq:POIQuery[_]):Source[String, Unit] =  {
      import POIQueries._ //Important for compilation order.
      import POIs._ //Important for compilation order as well.
      poiMap(pq)
    }
    def poiMap(pq:POIQuery[_]):Source[String, Unit] = macro gottaMatchEmAll[POIElement]

    def areaToGeoJson(el:AreaElement) = OsmDenormalizedSerializer.toGeoJsonString(el.osmObject)

    def toSource[A](shape:SourceGraph[A], serialise:A => String):Source[String, Unit] =
      Source.wrap(shape).via(serialiser(serialise))

    def sourceArea[A <: AreaElement](shape:SourceGraph[A]) = toSource(shape, areaToGeoJson)

    translatedQuery match {

        // FIXME: This should be simplified
      case \/-(CountryQuery(shape)) => sourceArea(shape)
      case \/-(StateQuery(shape)) => sourceArea(shape)
      case \/-(RegionQuery(shape)) => sourceArea(shape)
      case \/-(CityQuery(shape)) => sourceArea(shape)
      case \/-(TownshipQuery(shape)) => sourceArea(shape)
      case \/-(DistrictQuery(shape)) => sourceArea(shape)
      case \/-(VillageQuery(shape)) => sourceArea(shape)
      case \/-(CommunityQuery(shape)) => sourceArea(shape)

      case \/-(CoordinatesQuery(shape)) =>
        val serialise = (l:Location) =>  GeoJsonSerialiser.jsonFromFeature(Feature(l.point, Map.empty))
        toSource(shape, serialise)

      case \/-(p:POIQuery[_]) => poiMapImport(p)

      case -\/(ue: UnpickleError) =>
        Source.single("Your request was invalid")
      case -\/(qte @ QueryTranslationError(NotYetImplemented(q))) =>
        Source.single(s"Your query type $q is not yet implemented.")
      case -\/(qte @ QueryTranslationError(_)) =>
        Source.single(s"Could not process the query.")
    }
  }

  def toQuery(mat: Materializer, ec: ExecutionContext): (String) => Source[String, Unit] = (msg: String) => {

    val terminatorFlow: Flow[String, String, Unit] = Flow() { implicit b => {

      import FlowGraph.Implicits._

      val wsTerminator: WSTerminatorShape[String] = b add WSTerminator[String]

      val packS = b add Flow[String].map(identity)
      val unpackS = b add Flow[String].map(identity)

      packS ~> wsTerminator.in
      wsTerminator.out ~> unpackS.inlet
      (packS.inlet, unpackS.outlet)
    }}

    translate(msg)(mat, ec).via(terminatorFlow)
  }

  def serialiser[E](toString: (E) => String): Flow[E, String, Unit] = Flow[E]
    .map(toString)

  def pack: Flow[String, Strict, Unit] = Flow[String].map(TextMessage(_))

  def unpack: Flow[Message, String, Unit] = Flow[Message].log("Input")
    .collect[String] {
    case TextMessage.Strict(txt: String) => txt
  }

  def flattenQuery: Flow[Source[String, Unit], String, Unit] =
    Flow[Source[String, Unit]].flatten(FlattenStrategy.concat[String])

  def query(toQuery: (String) => Source[String, Unit])(implicit mat: Materializer, ec: ExecutionContext): Flow[Message, TextMessage, Unit] =

    Flow() { implicit b =>

      import FlowGraph.Implicits._

      val unpackS: FlowShape[Message, String] = b add unpack
      val packS  : FlowShape[String, Strict] = b add pack

      unpackS.log(s"Unpack") ~>
        Flow[String].log(s"Extract").map(toQuery).log(s"Query") ~>
        flattenQuery.log(s"Flattened") ~>
        packS

      (unpackS.inlet, packS.outlet)

    }
}
