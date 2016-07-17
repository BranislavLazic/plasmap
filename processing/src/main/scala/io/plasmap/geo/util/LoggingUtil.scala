package io.plasmap.geo.util

import com.typesafe.scalalogging.Logger
import io.plasmap.model.{OsmDenormalizedRelation, OsmDenormalizedWay, OsmRelation, _}
import io.plasmap.util.{GeowUtils, NormalisedOrDenormalised}
import org.slf4j.LoggerFactory

import scala.util.Try

object LoggingUtil {

  val log = Logger(LoggerFactory.getLogger("preprocessing"))

  def logFailure[T](logF: Throwable => Unit ) : Try[T] => Try[T] =
    intent => {
      intent match {
        case scala.util.Success(_) =>
        case scala.util.Failure(e) => logF(e)
      }
      intent
    }


  import GeowUtils._

  def osmRate[T: NormalisedOrDenormalised]: T => Long = {
    case _: OsmNode | _: OsmDenormalizedNode => 100000L
    case _: OsmWay | _: OsmDenormalizedWay => 10000L
    case _: OsmRelation | _: OsmDenormalizedRelation => 100L
  }

  def createLoggingCounter[T](rate: T => Long, log: Long => Unit, isCounted: T => Boolean = (_:T) => true): T => T = {

    var count = 0L

    elem => {
      if (count % rate(elem) == 0) {
        log(count)
      }
      if (isCounted(elem)) {
        count += 1
      }
      elem
    }
  }

  val parsedNodeCounter = createLoggingCounter[OsmObject](osmRate, count => log.trace(s"Parsed $count nodes"), isNode )
  val parsedWayCounter = createLoggingCounter[OsmObject](osmRate, count => log.trace(s"Parsed $count ways"), isWay )
  val parsedRelationCounter = createLoggingCounter[OsmObject](osmRate, count => log.trace(s"Parsed $count relations"), isRelation )

  val parsedCounter = parsedNodeCounter andThen parsedWayCounter andThen parsedRelationCounter

  val processedNodeCounter = createLoggingCounter[OsmDenormalizedObject](osmRate, count => log.trace(s"Processed $count nodes"), isDenormalisedNode )
  val processedWayCounter = createLoggingCounter[OsmDenormalizedObject](osmRate, count => log.trace(s"Processed $count ways"), isDenormalisedWay )
  val processedRelationCounter = createLoggingCounter[OsmDenormalizedObject](osmRate, count => log.trace(s"Processed $count relations"), isDenormalisedRelation )

  val processedCounter = processedNodeCounter andThen processedWayCounter andThen processedRelationCounter

  val producedNodeCounter = createLoggingCounter[OsmObject](osmRate, count => log.trace(s"Produced $count nodes"), isNode )
  val producedWayCounter = createLoggingCounter[OsmObject](osmRate, count => log.trace(s"Produced $count ways"), isWay )
  val producedRelationCounter = createLoggingCounter[OsmObject](osmRate, count => log.trace(s"Produced $count relations"), isRelation )

  val producedCounter = producedNodeCounter andThen producedWayCounter andThen producedRelationCounter

}
