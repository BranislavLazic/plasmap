package io.plasmap.geo.mappings

import akka.actor.{Actor, Props}
import io.plasmap.geo.mappings.impl.ElasticIndexingService
import io.plasmap.model.OsmDenormalizedObject

import akka.pattern.pipe

/**
 * Untyped actor for indexing an osm object.
 *
 * Created by mark on 10.02.15.
 *
 * @author Jan Schulte <jan@plasmap.io>
 *
 */
object IndexingActor {

  def props():Props = Props[IndexingActor]
}

class IndexingActor extends Actor {
  import IndexingActor._
  import context.dispatcher

  lazy val service = IndexingService()

  override def receive: Receive = {
    case mapping:IndexMapping ⇒
      val replyAddress = sender()
      service.indexOsmObject(mapping) pipeTo replyAddress
  }
}
