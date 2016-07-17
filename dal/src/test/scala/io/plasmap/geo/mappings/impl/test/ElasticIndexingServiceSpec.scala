package io.plasmap.geo.mappings.impl.test

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticClient
import io.plasmap.geo.mappings.IndexMapping
import io.plasmap.geo.mappings.impl.ElasticIndexingService
import io.plasmap.util.test.OsmGenerators
import OsmGenerators._
import io.plasmap.model._
import io.plasmap.model.geometry.{GeometryCollection, LineString, Point}
import org.elasticsearch.common.settings.ImmutableSettings
import org.scalacheck.Prop.forAll
import org.scalatest.Retries
import org.specs2.ScalaCheck
import org.specs2.concurrent._
import org.specs2.matcher.{FutureMatchers, ResultMatchers, Scope}
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.reflect.io.File
import scalaz.\/

/**
 * Created by mark on 11.02.15.
 */
object ElasticIndexingServiceSpec {
  val elasticPath = "geo-mappings/src/test/resources/elastic"
  lazy val settings = ImmutableSettings.settingsBuilder()
    .put("http.enabled", false)
    .put("path.home", elasticPath)

  implicit val system = ActorSystem()
  lazy val service = new ElasticIndexingService(ElasticClient.local(settings.build()))
  val retries:Int = 2
  val timeout:FiniteDuration = 7.seconds

}

class ElasticIndexingServiceSpec(implicit ev:ExecutionEnv) extends Specification with NoTimeConversions with ScalaCheck with ResultMatchers with Retries with FutureMatchers {
  import ElasticIndexingServiceSpec._

  val unitTest = true
  skipAllIf(unitTest)

  "The ElasticSearchServiceSpec" should {
    sequential

    "successfully store generated OsmNodes" in new Scope {
      forAll(osmDenormalizedNodeG) { (node: OsmDenormalizedNode) ⇒
        val tagMap = node.tags.map((tag) => tag.key -> tag.value).toMap
        val indexMapping = IndexMapping(node,tagMap)
        service.indexOsmObject(indexMapping) must beSome(indexMapping).await(retries, timeout)
      }
    }

    "successfully store generated OsmWays" in {
      forAll(osmDenormalizedWayG) { (way: OsmDenormalizedWay) ⇒
        val tagMap = way.tags.map((tag) => tag.key -> tag.value).toMap
        val indexMapping = IndexMapping(way,tagMap)
        service.indexOsmObject(indexMapping) must beSome(indexMapping).await(retries, timeout)
      }
    }

    "successfully store generated OsmRelations" in {
      forAll(osmDenormalizedRelationG) { (relation: OsmDenormalizedRelation) ⇒
        val tagMap = relation.tags.map((tag) => tag.key -> tag.value).toMap
        val indexMapping = IndexMapping(relation,tagMap)
        service.indexOsmObject(indexMapping) must beSome(indexMapping).await(retries, timeout)
      }
    }

    "successfully retrieve stored values" in {
      val tags = List(OsmTag("addr:street", "Merowingerplatz"))
      val n = osmDenormalizedNodeG.sample.get
      val w = osmDenormalizedWayG.sample.get
      val r = OsmDenormalizedRelation(OsmId(1234L), None, OsmVersion(), tags, GeometryCollection(List(
        Point(1L),
        LineString( List(
          (1.0, 2.0),(2.0, 2.0),(2.0, 1.0),(1.0, 2.0)
        )))))
      // val r = osmDenormalizedRelationG.sample.get.copy(tags = tags) doesn't work
      val tagMap = tags.map((tag) => tag.key -> tag.value).toMap
      val all = for (o ← List(n,w,r)) yield service.indexOsmObject(IndexMapping(o,tagMap))
      val indexingF = Future.sequence(all)
      Await.ready(indexingF, 4.seconds)

      val queryF = service.queryForOsmObject("Merowingerplatz").map(_.headOption)

      queryF.map(_.map(_.id)) must beSome(r.id).await(retries, timeout)
    }

  }

  step { //Cleanup
    \/.fromTryCatchNonFatal{
      println("Deleting " + File(elasticPath).toAbsolute)
      File(elasticPath).deleteRecursively()
    }
  }
}


