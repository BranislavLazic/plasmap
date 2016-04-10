package io.plasmap.geo.data

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import io.plasmap.geo.data.OsmStorageActor._
import io.plasmap.model.{OsmId, OsmTag, OsmType}

object OsmStorageActor {

  case class StoreBB(elementBB: OsmBB)

  case class EntryBB(elementBB: Option[OsmBB])

  case class GetBB(bb: Long, osmId: OsmId, typ: OsmType)

  case class CountBB(bb: Long, typ: OsmType)

  case class StoreBBTag(elementBBTag: OsmBBTag)

  case class EntryBBTag(elementBBTag: Option[OsmBBTag])

  case class EntriesBBTag(entries: List[OsmBBTag])

  case class GetBBTag(bb: Long, osmId: OsmId, tag: OsmTag, typ: OsmType)

  case class GetElementsByBBandTag(bb: Long, tag: OsmTag, typ: OsmType)

  case class CountBBTag(bb: Long, tag: OsmTag, typ: OsmType)

  def props(): Props = Props[OsmStorageActor]
}

class OsmStorageActor(service: OsmStorageService) extends Actor with ActorLogging {

  def this() = this(OsmStorageService())

  import context.dispatcher

  def receive = {
    case GetBB(bb, osmId, typ) => {
      log.debug(s"Received get request for bb $bb, osmdId $osmId and type $typ")
      val replyAddress = sender()
      service.findBB(bb, osmId, typ).map(EntryBB) pipeTo replyAddress
    }
    case CountBB(bb, typ) => {
      log.debug(s"Received count request for bb $bb and type $typ")
      val replyAddress = sender()
      service.countBB(bb, typ) pipeTo replyAddress
    }
    case StoreBB(elementBB) => {
      log.debug(s"Received store request for bb ${elementBB.bb} and ${elementBB.osmId}")
      val replyAddress = sender()
      service.insertBB(elementBB).map(elementBB => EntryBB(elementBB)) pipeTo replyAddress
    }
    case GetBBTag(bb, osmId, tag, typ) => {
      log.debug(s"Received single-get request for bb $bb, osmdId $osmId, tag $tag and type $typ")
      val replyAddress = sender()
      service.findSingleBBTag(bb, osmId, tag, typ) pipeTo replyAddress
    }
    case GetElementsByBBandTag(bb, tag, typ) => {
      log.debug(s"Received multi-get request for bb $bb, tag $tag and type $typ")
      val replyAddress = sender()
      service.findBBTag(bb, tag, typ).map(x => { log.debug(x.toString()); EntriesBBTag(x)}) pipeTo replyAddress
    }
    case CountBBTag(bb, tag, typ) => {
      log.debug(s"Received count request for bb $bb, tag $tag and type $typ")
      val replyAddress = sender()
      service.countBBTag(bb, tag, typ) pipeTo replyAddress
    }
    case StoreBBTag(elementBBTag) => {
      log.debug(s"Received store request for bb ${elementBBTag.bb}, tag ${elementBBTag.tag} and ${elementBBTag.osmId}")
      val replyAddress = sender()
      service.insertBBTag(elementBBTag).map(elementBBTag => EntryBBTag(elementBBTag)) pipeTo replyAddress
    }
    case s => {
      log.debug(s"Got unknown message $s")
    }
  }
}
