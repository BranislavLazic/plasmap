package io.plasmap.geo.mappings

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import io.plasmap.geo.mappings.impl.ElasticIndexingService
import io.plasmap.model.{OsmDenormalizedObject, OsmId, OsmType}

import scala.concurrent.{ExecutionContext, Future}


sealed trait IndexMessage
case class IndexSearchHit(id: OsmId, score: Float) extends IndexMessage
case class IndexMapping(osm: OsmDenormalizedObject, tags:Map[String,String])

/**
 * Service trait for indexing osm objects.
 *
 * @author Jan Schulte <jan@plasmap.io>
 */
trait IndexingService {

  def indexOsmObjectSink(batchSize: Int = 100, concurrentBatches: Int = 4, successFn:(OsmId) => Unit, errorHandler: (Throwable) => Unit): Sink[IndexMapping, NotUsed]

  def indexOsmObject(mapping:IndexMapping)(implicit ec: ExecutionContext): Future[Option[IndexMapping]]

  def searchOsmObjectSource(queryTerm: String, typ: OsmType): Source[IndexSearchHit, NotUsed]

  def queryForOsmObject(queryTerm: String, typ: OsmType)(implicit ec: ExecutionContext): Future[List[IndexSearchHit]]

  def queryForOsmObject(queryTerm: String)(implicit ec: ExecutionContext): Future[List[IndexSearchHit]]


}


object IndexingService {

  def apply():IndexingService = ElasticIndexingService()
}
