package io.plasmap.geo.mappings.impl

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Source, Sink}
import com.sksamuel.elastic4s.streams.{ResponseListener, ScrollPublisher, BulkIndexingSubscriber, RequestBuilder}
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import io.plasmap.driver.elasticsearch.Indexer
import io.plasmap.geo.mappings.{IndexMapping, IndexSearchHit, IndexingService}
import io.plasmap.model._
import org.elasticsearch.action.bulk.BulkItemResponse
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.SearchHit
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scalaz.ListT
import scalaz.std.AllInstances._
import scalaz.syntax.id._
import com.sksamuel.elastic4s.streams.ReactiveElastic._

/**
 * Takes osm objects and indexes them with elastic search.
 *
 * @author mark@plasmap.io
 */

object ElasticIndexingService {

  private val config = ConfigFactory.load()
  private val hostConfig: String = config.getString("plasmap.indexer.io.plasmap.geo.mappings.impl.elasticsearch.host")
  private val portConfig: Int = config.getInt("plasmap.indexer.io.plasmap.geo.mappings.impl.elasticsearch.port")

  val configIndexer = Indexer(hostConfig, portConfig)

  private val log = Logger(LoggerFactory.getLogger(ElasticIndexingService.getClass.getName))
  log.info(s"${Console.GREEN}Created OsmIndexingService[host=$hostConfig,port=$portConfig]${Console.RESET}")


  private val NodesIndexName = "nodes"
  private val WaysIndexName = "ways"
  private val RelationsIndexName = "relations"

  private val system = ActorSystem("elastic")

  def apply() = new ElasticIndexingService(ElasticIndexingService.configIndexer)(system)

}

class ElasticIndexingService(elasticClient: ElasticClient)(implicit val system:ActorSystem) extends IndexingService {

  import ElasticIndexingService._

  private def indexName(osm: OsmDenormalizedObject): String = {
    osm match {
      case _: OsmDenormalizedNode ⇒ NodesIndexName
      case _: OsmDenormalizedWay ⇒ WaysIndexName
      case _: OsmDenormalizedRelation ⇒ RelationsIndexName
    }
  }


  override def indexOsmObject(mapping: IndexMapping)(implicit ec: ExecutionContext): Future[Option[IndexMapping]] = {
    val osm: OsmDenormalizedObject = mapping.osm
    val osmId = osm.id.value
    val tags = mapping.tags

    if (tags.isEmpty) {
      Future {
        Some(mapping)
      }
    } else {
      val indexingQuery = index into indexName(osm) id osmId fields tags
      val indexResponse = elasticClient.execute(indexingQuery)
      indexResponse.map { ir ⇒ {
        log.trace(s"Index for $mapping successfully created.")
        Some(mapping)
      }
      }
    }
  }

  implicit val builder = new RequestBuilder[IndexMapping] {

    import ElasticDsl._

    // the request returned doesn't have to be an index - it can be anything supported by the bulk api
    def request(mapping: IndexMapping): BulkCompatibleDefinition = {
      val osm: OsmDenormalizedObject = mapping.osm
      val osmId = osm.id.value
      val tags = mapping.tags

      index into indexName(osm) id osmId fields tags
    }
  }

  override def indexOsmObjectSink(batchSize: Int = 100, concurrentBatches: Int = 4, successFn:(OsmId) => Unit, errorHandler: (Throwable) => Unit): Sink[IndexMapping, Unit] = {

    val listener = new ResponseListener {
      override def onAck(resp: BulkItemResponse): Unit = {

        if(!resp.isFailed) {
          val id: Long = resp.getResponse[IndexResponse].getId.toLong
          successFn(OsmId(id))
        }else{
          // TODO: What happens if there is a failure here when there shouldn't be one?
        }
      }
    }
    Flow[IndexMapping].filter(_.tags.nonEmpty).to(Sink(elasticClient.subscriber[IndexMapping] (batchSize, concurrentBatches, listener = listener, errorFn = errorHandler)))
  }

  override def searchOsmObjectSource(queryTerm: String, typ: OsmType): Source[IndexSearchHit, Unit] = {
    val index = typ match {
      case OsmTypeNode => NodesIndexName
      case OsmTypeWay => WaysIndexName
      case OsmTypeRelation => RelationsIndexName
    }
    val searchQuery = search in index query queryTerm scroll "50ms"
    val publisher = elasticClient.publisher(searchQuery)
    Source(publisher)
      .map((hit) => IndexSearchHit(OsmId(hit.id.toLong), hit.score))
  }



  /** Queries a given index for a term */
  private def searchAndGetHits(index: String, queryTerm: String)(implicit ec: ExecutionContext): FutureList[IndexSearchHit] = {
    for {
      result: SearchResponse ← elasticClient.execute {
        search in index query queryTerm
      } |> futureAsFutureList
      hit: SearchHit ← result.getHits.getHits |> arrayAsFutureList
    } yield {
      log.trace(s"Found element $hit")
      val fields = hit.getFields
      IndexSearchHit(OsmId(hit.getId.toLong), hit.getScore)
    }
  }

  override def queryForOsmObject(queryTerm: String, typ: OsmType)(implicit ec: ExecutionContext): Future[List[IndexSearchHit]] = typ match {
    case OsmTypeNode => searchAndGetHits(NodesIndexName, queryTerm).run
    case OsmTypeWay => searchAndGetHits(WaysIndexName, queryTerm).run
    case OsmTypeRelation => searchAndGetHits(RelationsIndexName, queryTerm).run
  }

  override def queryForOsmObject(queryTerm: String)(implicit ec: ExecutionContext): Future[List[IndexSearchHit]] = {
    (searchAndGetHits(RelationsIndexName, queryTerm) ++
      searchAndGetHits(WaysIndexName, queryTerm) ++
      searchAndGetHits(NodesIndexName, queryTerm)
      ).run // .run turns a FutureList[A] into a Future[List[A]]
  }

  /** Type that allows us to use Future[List[A]] in a for comprehension */
  type FutureList[A] = ListT[Future, A]

  def futureAsFutureList[A](fut: Future[A])(implicit ec: ExecutionContext): FutureList[A] = ListT[Future, A] (
fut.map (List (_) )
)

  def arrayAsFutureList[A](arr: Array[A])(implicit ec: ExecutionContext): FutureList[A] = ListT[Future, A] (
Future {
arr.toList
}
)
}
