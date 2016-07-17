package io.plasmap.query.engine.test

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import io.plasmap.query.engine._
import io.plasmap.querymodel.{PMCityFromCoordinates, PMCoordinates, PMDistrictsFromArea}
import org.scalamock.proxy.ProxyMockFactory
import org.scalamock.specs2.IsolatedMockFactory
import org.specs2.mutable.Specification

import scala.util.Random



/**
 *
 * @author Jan Schulte <jan@plasmap.io>
 */
object QueryTranslatorSpec extends Specification with IsolatedMockFactory with ProxyMockFactory {


  implicit val system = ActorSystem("test")
  implicit val mat = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits.global

  val gen = new Random()

  "The QueryTranslator" should {

    val lon = gen.nextDouble()
    val lat = gen.nextDouble()

    "translate a \"PMCityFromCoordinates\" query" in {

      val pmQuery = PMCityFromCoordinates(PMCoordinates(lon,lat))

      val translatedQuery = QueryTranslator.translate(pmQuery)
      translatedQuery.toEither must beRight

    }
    "translate a \"PMDistrictsFromCity\" query" in {

      val pmCity = PMCityFromCoordinates(PMCoordinates(lon,lat))
      val pmDistrict = PMDistrictsFromArea(pmCity)

      val translatedQuery = QueryTranslator.translate(pmDistrict)
      println(s"Got $translatedQuery")
      translatedQuery.toEither must beRight

    }
  }
}