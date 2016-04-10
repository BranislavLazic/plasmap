package io.plasmap.geo.preprocessing

import java.io.{PrintWriter, StringWriter}

import io.plasmap.model.{OsmId, OsmObject, OsmRelation, OsmWay}


sealed trait FlowError {
  def t: Throwable

  override def toString: String = {
    val sw = new StringWriter
    t.printStackTrace(new PrintWriter(sw))

    s"Failed to process data. \nStacktrace: ${sw.toString}"
  }

}

final case class CouldNotDenormaliseObject(osmObject: OsmObject, t: Throwable) extends FlowError

final case class IndexPersisterError(t: Throwable) extends FlowError

final case class MappingPersisterError(t: Throwable) extends FlowError

final case class DataByTagPersisterError(t: Throwable) extends FlowError

final case class DataPersisterError(t: Throwable) extends FlowError