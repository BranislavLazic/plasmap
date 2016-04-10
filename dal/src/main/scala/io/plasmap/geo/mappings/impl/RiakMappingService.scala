package io.plasmap.geo.mappings.impl

import akka.util.ByteString
import com.basho.riak.client.api.RiakClient
import com.basho.riak.client.api.commands.kv.{FetchValue, StoreValue}
import com.basho.riak.client.core.query.{RiakObject, Location, Namespace}
import com.basho.riak.client.core.util.BinaryValue
import com.typesafe.config.ConfigFactory
import io.plasmap.geo.mappings._
import io.plasmap.model.OsmId
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.{Future, ExecutionContext}
import scalaz.\/


/**
 * @author Jan Schulte <jan@plasmap.io>
 */
final class RiakMappingService extends MappingService {

  import MappingSerializer._
  private val config = ConfigFactory.load()
  private val hostName: String = config.getString("plasmap.db.riak.host")
  private val bucketName: String = config.getString("plasmap.db.riak.osmBucket")
  private val client = RiakClient.newClient(hostName)
  private val nodesNS = new Namespace("nodes", bucketName)
  private val waysNS  = new Namespace("ways", bucketName)
  private val relsNS  = new Namespace("relations", bucketName)

  def prepareInsertion(id:Long, ns:Namespace, value:Array[Byte]) = {
    val loc = new Location(ns, s"$id")
    val obj = new RiakObject()
    obj setValue ( BinaryValue create value )
    val store = new StoreValue.Builder(obj).withLocation(loc).build()
    client execute store
  }

  def insertMapping[A <: OsmMapping](mapping:A, serializer:A => ByteString):Future[Option[A]] = {
    Future{
      val value = serializer(mapping).toArray
      val namespace = mapping match { case _:OsmNodeMapping => nodesNS case _:OsmWayMapping => waysNS case _ => relsNS}
      val resp = prepareInsertion(mapping.osmId.value, namespace, value)
      if(resp.hasValues) Some(mapping) else None
    }
  }

  def getMapping[A <: OsmMapping](osmId:OsmId, namespace:Namespace, deserializer:ByteString => A):Future[Option[A]] = {
    Future {
      val id   = osmId.value
      val loc  = new Location(namespace, s"$id")
      val fv   = new FetchValue.Builder(loc).build
      val resp = client execute fv
      if(resp.isNotFound) None
      else {
        \/.fromTryCatchNonFatal{
          val value = resp.getValue(classOf[RiakObject]).getValue.getValue //Oh yeah.
          deserializer(ByteString(value))
        }.toOption
      }
    }
  }

  //Insertion
  override def insertNodeMapping(mapping: OsmNodeMapping)(implicit ec: ExecutionContext): Future[Option[OsmNodeMapping]] =
    insertMapping(mapping, nodeMappingSerializer.serialize)

  override def insertWayMapping(mapping: OsmWayMapping)(implicit ec: ExecutionContext): Future[Option[OsmWayMapping]] =
    insertMapping(mapping, wayMappingSerializer.serialize)

  override def insertRelationMapping(mapping: OsmRelationMapping)(implicit ec: ExecutionContext): Future[Option[OsmRelationMapping]] =
    insertMapping(mapping, relationMappingSerializer.serialize)

  //Finding
  override def findNodeMapping(osmId: OsmId)(implicit ec: ExecutionContext): Future[Option[OsmNodeMapping]] =
    getMapping(osmId, nodesNS, nodeMappingSerializer.deserialize)

  override def findWayMapping(osmId: OsmId)(implicit ec: ExecutionContext): Future[Option[OsmWayMapping]] =
    getMapping(osmId, waysNS, wayMappingSerializer.deserialize)

  override def findRelationMapping(osmId: OsmId)(implicit ec: ExecutionContext): Future[Option[OsmRelationMapping]] =
    getMapping(osmId, relsNS, relationMappingSerializer.deserialize)

}
