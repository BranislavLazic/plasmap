package io.plasmap.query.engine

import _root_.io.plasmap.queryengine.macros.Macros.GeneratePOIQueries
import io.plasmap.geo.data.OsmStorageService
import _root_.io.plasmap.geo.mappings.{IndexSearchHit, IndexingService, MappingService}
import io.plasmap.geohash.{GeoHash, PrecisionVeryLow_80KM}
import _root_.io.plasmap.model._
import _root_.io.plasmap.model.geometry.{LonLatPoint, Point}
import io.plasmap.util.GeoCalculator
import _root_.io.plasmap.util.streams.{StreamAdditions, Utilities}
import akka.stream._
import akka.stream.scaladsl.FlowGraph.Builder
import akka.stream.scaladsl.FlowGraph.Implicits._
import akka.stream.scaladsl._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

import StreamAdditions._

sealed trait Query[+S <: Shape, +M] {
  def shape: Graph[S, M]
}

object TypeAliases {
  type SourceGraph[A] = Graph[SourceShape[A], Unit]
}

import TypeAliases._


sealed trait POIQuery[A] extends Query[SourceShape[A], Unit] {
  def shape: SourceGraph[A]
}

@GeneratePOIQueries[POIElement]("POIQuery") object POIQueries

case class CoordinatesQuery(shape: SourceGraph[Location]) extends Query[SourceShape[Location], Unit]
object CoordinatesQuery {
  def apply(lon: Double, lat: Double): CoordinatesQuery = CoordinatesQuery(Queries.location(lon, lat))
}

//FIXME: Create macro for area queries
sealed trait AreaQuery[A <: AreaElement] extends Query[SourceShape[A], Unit]

case class CountryQuery(shape: SourceGraph[Country]) extends AreaQuery[Country]
object CountryQuery {

  import Queries._

  def apply(lon: Double, lat: Double): CountryQuery = CountryQuery(relationByCoordinatesAndTypeShape(lon,lat,countryTag,Country))
  def apply(name: String): CountryQuery = CountryQuery(relationByNameAndTypeShape(name, countryTag,Country))

}

case class StateQuery(shape: SourceGraph[State]) extends AreaQuery[State]
object StateQuery {

  import Queries._

  def apply[T <: AreaElement](areaQuery: AreaQuery[T])(implicit mat: Materializer, ec: ExecutionContext): StateQuery =
    StateQuery(relationByContainment(areaQuery,stateTag,State)(mat, ec))
  def apply(lon: Double, lat: Double): StateQuery = StateQuery(relationByCoordinatesAndTypeShape(lon,lat,stateTag,State))
  def apply(name: String): StateQuery = StateQuery(relationByNameAndTypeShape(name, stateTag,State))

}

case class RegionQuery(shape: SourceGraph[Region]) extends AreaQuery[Region]
object RegionQuery {

  import Queries._

  def apply[T <: AreaElement](areaQuery: AreaQuery[T])(implicit mat: Materializer, ec: ExecutionContext): RegionQuery =
    RegionQuery(relationByContainment(areaQuery,regionTag,Region)(mat, ec))
  def apply(lon: Double, lat: Double): RegionQuery = RegionQuery(relationByCoordinatesAndTypeShape(lon,lat,regionTag,Region))
  def apply(name: String): RegionQuery = RegionQuery(relationByNameAndTypeShape(name, regionTag,Region))

}

case class CityQuery(shape: SourceGraph[City]) extends AreaQuery[City]
object CityQuery {

  import Queries._

  def apply[T <: AreaElement](areaQuery: AreaQuery[T])(implicit mat: Materializer, ec: ExecutionContext): CityQuery =
    CityQuery(relationByContainment(areaQuery,cityTag,City)(mat, ec))
  def apply(lon: Double, lat: Double): CityQuery = CityQuery(relationByCoordinatesAndTypeShape(lon,lat,cityTag,City))
  def apply(name: String): CityQuery = CityQuery(relationByNameAndTypeShape(name, cityTag,City))

}

case class TownshipQuery(shape: SourceGraph[Township]) extends AreaQuery[Township]
object TownshipQuery {

  import Queries._

  def apply[T <: AreaElement](areaQuery: AreaQuery[T])(implicit mat: Materializer, ec: ExecutionContext): TownshipQuery =
    TownshipQuery(relationByContainment(areaQuery,townshipTag,Township)(mat, ec))
  def apply(lon: Double, lat: Double): TownshipQuery = TownshipQuery(relationByCoordinatesAndTypeShape(lon,lat,townshipTag,Township))
  def apply(name: String): TownshipQuery = TownshipQuery(relationByNameAndTypeShape(name, townshipTag,Township))

}

case class DistrictQuery(shape: SourceGraph[District]) extends AreaQuery[District]
object DistrictQuery {

  import Queries._

  def apply[T <: AreaElement](areaQuery: AreaQuery[T])(implicit mat: Materializer, ec: ExecutionContext): DistrictQuery =
    DistrictQuery(relationByContainment(areaQuery,districtTag,District)(mat, ec))
  def apply(lon: Double, lat: Double): DistrictQuery = DistrictQuery(relationByCoordinatesAndTypeShape(lon,lat,districtTag,District))
  def apply(name: String): DistrictQuery = DistrictQuery(relationByNameAndTypeShape(name, districtTag,District))
}

case class VillageQuery(shape: SourceGraph[Village]) extends AreaQuery[Village]
object VillageQuery {

  import Queries._

  def apply[T <: AreaElement](areaQuery: AreaQuery[T])(implicit mat: Materializer, ec: ExecutionContext): VillageQuery =
    VillageQuery(relationByContainment(areaQuery,villageTag,Village)(mat, ec))
  def apply(lon: Double, lat: Double): VillageQuery = VillageQuery(relationByCoordinatesAndTypeShape(lon,lat,villageTag,Village))
  def apply(name: String): VillageQuery = VillageQuery(relationByNameAndTypeShape(name, villageTag,Village))

}

case class CommunityQuery(shape: SourceGraph[Community]) extends AreaQuery[Community]
object CommunityQuery {

  import Queries._

  def apply[T <: AreaElement](areaQuery: AreaQuery[T])(implicit mat: Materializer, ec: ExecutionContext): CommunityQuery =
    CommunityQuery(relationByContainment(areaQuery,communityTag,Community)(mat, ec))
  def apply(lon: Double, lat: Double): CommunityQuery = CommunityQuery(relationByCoordinatesAndTypeShape(lon,lat,communityTag,Community))
  def apply(name: String): CommunityQuery = CommunityQuery(relationByNameAndTypeShape(name, communityTag,Community))

}


object PointOfInterestQuery {

  import Queries._

  def apply[A <: AreaElement, B: POI](coordinatesQuery: CoordinatesQuery, toData: (BoundingBox, Tag) ⇒ Future[List[OsmDenormalizedObject]] = retrieveNodeData)
                                     (implicit mat: Materializer, ec: ExecutionContext): POIQuery[B] =
  {
    val Poi = implicitly[POI[B]]
    Poi.queryFromShape(FlowGraph.partial() {

      implicit builder: Builder[Unit] ⇒
        val source: Source[Location, Unit] = Source.wrap(coordinatesQuery.shape)

        // ohh what a beauty..
        val subFlow: Flow[Location, B, Unit] = Flow[Location]
          .map(_.point)
          .mapConcat((point) => Poi.tags.map((t) => point -> t))
          .map((tuple) => createBBTag(tuple._1,tuple._2))
          .mapAsync(4)(toData.tupled)
          .mapConcat(identity)
          .map(Poi.fromOsmDenObj)

        val flow: Flow[Location, (Location, List[B]), Unit] = Utilities.groupAndMap[Location, Location, B](100, identity, subFlow)(mat, ec)
        val groupedSource: Source[(Location, List[B]), Unit] = source.via(flow)
        val filteredSource: Source[B, Unit] =
          Utilities
            .flatten(groupedSource)(mat)
            //.filter { case (area, poi) ⇒ GeoCalculator.within(Poi.osmObj(poi).geometry, area.osmObject.geometry) }
            .map(_._2)

        builder.add(filteredSource): SourceShape[B]
    }.named(Poi.name))
  }

  def apply[A <: AreaElement, B: POI](areaQuery: AreaQuery[A])
                                     (implicit mat: Materializer, ec: ExecutionContext): POIQuery[B] =
    fromArea(areaQuery)(implicitly[POI[B]], mat, ec)

  def fromArea[A <: AreaElement, B: POI](areaQuery: AreaQuery[A], toData: (BoundingBox, Tag) ⇒ Future[List[OsmDenormalizedObject]] = retrieveNodeData)
                                        (implicit mat: Materializer, ec: ExecutionContext): POIQuery[B] = {
    val Poi = implicitly[POI[B]]
    Poi.queryFromShape(FlowGraph.partial() {

      implicit builder: Builder[Unit] ⇒
        val source: Source[A, Unit] = Source.wrap(areaQuery.shape)

        // ohh what a beauty..
        val subFlow: Flow[A, B, Unit] = Flow[A]
          .map(_.osmObject)
          .mapConcat(Poi.bbsToQuery)
          .mapAsync(4)(toData.tupled)
          .mapConcat(identity)
          .map(Poi.fromOsmDenObj)

        val flow: Flow[A, (A, List[B]), Unit] = Utilities.groupAndMap[A, A, B](100, identity, subFlow)(mat, ec)
        val groupedSource: Source[(A, List[B]), Unit] = source.via(flow)
        val filteredSource: Source[B, Unit] =
          Utilities
            .flatten(groupedSource)(mat)
            .filter { case (area, poi) ⇒ GeoCalculator.within(Poi.osmObj(poi).geometry, area.osmObject.geometry) }
            .map(_._2)

        builder.add(filteredSource): SourceShape[B]
    }.named(Poi.name))
  }
}

/**
 * Definition of high-level queries.
 *
 * @author Jan Schulte <jan@plasmap.io>
 */
object Queries {

  lazy val indexingService = IndexingService()

  private[engine] def relationByNameAndType(name: String,
                                            filterTag: OsmTag,
                                            toIndex: (Name) => Future[List[Id]] = retrieveRelationId,
                                            toBoundingBox: (Id) => Future[List[BoundingBox]] = retrieveRelationBB,
                                            toData: (BoundingBox, Id) => Future[List[OsmDenormalizedObject]]  = retrieveRelationById
                                             ): Source[OsmDenormalizedRelation, Unit] = {
    val indexSource: Source[IndexSearchHit, Unit] = indexingService.searchOsmObjectSource(name, OsmTypeRelation)
    val indexedSource = indexSource.map((ish) => Id(ish.id))

    val boundingBoxIdSource: Source[(BoundingBox, Id), Unit] = Utilities
      .mapConcatAndGroupAsync(indexedSource, toBoundingBox)
      .map(_.swap)

    val relationSource: Source[OsmDenormalizedRelation, Unit] = Utilities
              .mapConcatAndGroupAsync[(BoundingBox, Id), OsmDenormalizedObject, Unit](boundingBoxIdSource, toData.tupled)
      .collect { case ((bb, id), relation: OsmDenormalizedRelation) => relation }
      .filter(_.tags.contains(filterTag))
      .deduplicate(1000, 0.001)
    relationSource
  }

  private[engine] def relationByNameAndTypeShape[T <: AreaElement](name:String,tag:OsmTag,mapF:(OsmDenormalizedRelation) => T): Graph[SourceShape[T], Unit] =
    FlowGraph.partial() { implicit builder: Builder[Unit] =>
        val source: Source[T, Unit] = relationByNameAndType(name, tag).map(mapF)
      builder.add(source) : SourceShape[T]
    }.named("fromName")
  
  private[engine] def relationByCoordinatesAndType(lon: Double, lat: Double, tag: OsmTag, toData: (BoundingBox, Tag) => Future[List[OsmDenormalizedObject]] = retrieveRelationData): Source[OsmDenormalizedRelation, Unit] = {
    val source = location(lon, lat)
    val flow = Flow[Location]
      .map(_.point)
      .map((point) => createBBTag(point, tag))
      .mapAsync(4)(toData.tupled)
      .mapConcat(identity)
      .filter(osmObject => GeoCalculator.within(LonLatPoint(lon, lat), osmObject.geometry
      ))
      .collect { case rel: OsmDenormalizedRelation => rel }
    source.via(flow)
  }
  
  private[engine] def relationByCoordinatesAndTypeShape[T <: AreaElement](lon:Double,lat:Double,tag:OsmTag,mapF:(OsmDenormalizedRelation)=>T): Graph[SourceShape[T], Unit] =
    FlowGraph.partial() { implicit builder: Builder[Unit] =>
        val source: Source[T, Unit] = relationByCoordinatesAndType(lon, lat, tag).map(mapF)
      builder.add(source) : SourceShape[T]
    }.named("fromLonLat")

  private[engine] def relationByContainment[I <: AreaElement, O <: AreaElement](areaQuery: AreaQuery[I], tag:OsmTag,mapF:(OsmDenormalizedRelation)=> O,
                                              toData: (BoundingBox, Tag) => Future[List[OsmDenormalizedObject]] = retrieveRelationData)(mat: Materializer, ec: ExecutionContext): Graph[SourceShape[O], Unit] = {
    FlowGraph.partial() {
      implicit builder: Builder[Unit] =>


        val relationFlow: Flow[I, OsmDenormalizedRelation, Unit] = Flow[I]
          .map(_.osmObject)

        val subFlow: Flow[I, O, Unit] = relationFlow
          .mapConcat(createBBTag(_, tag))
          .deduplicate(10000, 0.001)
          .mapAsync(4)(toData.tupled)
          .mapConcat(identity)
          .collect { case relation: OsmDenormalizedRelation => mapF(relation) }

        val flow: Flow[I, (I, List[O]), Unit] = Utilities.groupAndMap[I, I, O](100, identity, subFlow)(mat, ec)
        val groupedSource: Source[(I, List[O]), Unit] = Source.wrap(areaQuery.shape).via(flow)

        val filteredSource = Utilities
          .flatten(groupedSource)(mat)
          .filter((tuple) => GeoCalculator.within(tuple._2.osmObject.geometry, tuple._1.osmObject.geometry))
          .map(_._2)
          .deduplicate(1000, 0.001)

        builder.add(filteredSource): SourceShape[O]
    }.named("fromArea")
  }

  private[engine] val countryTag = OsmTag("admin_level", "2")
  private[engine] val stateTag = OsmTag("admin_level", "4")
  private[engine] val regionTag = OsmTag("admin_level", "5")
  private[engine] val cityTag = OsmTag("admin_level", "6")
  private[engine] val townshipTag = OsmTag("admin_level", "7")
  private[engine] val districtTag = OsmTag("admin_level", "8")
  private[engine] val villageTag = OsmTag("admin_level", "9")
  private[engine] val communityTag = OsmTag("admin_level", "10")

  private[engine] def createBBTag(point: Point, adminLevel: OsmTag): (BoundingBox, Tag) = {
    val tag = Tag(adminLevel)
    val hash = point.hash
    val bb = BoundingBox(GeoHash.ultraHigh.reduceParallelPrecision(hash, PrecisionVeryLow_80KM))
    bb -> tag
  }

  private[engine] def createBBTag(rel: OsmDenormalizedRelation,tag: OsmTag): List[(BoundingBox, Tag)] = {

    val rectangles: List[(Point, Point)] = GeoCalculator.rectangle(rel.geometry)
    val matrices: List[Array[Array[Long]]] = rectangles.map((rectangle) => {
      val (upperLeft, lowerRight) = rectangle
      GeoHash.veryLow.encapsulatingRectangleHashes(upperLeft.hash, lowerRight.hash)
    })

    val elements = for {
      matrix <- matrices
      line <- matrix
      hash <- line
    } yield (BoundingBox(hash), Tag(tag))
    elements.distinct
  }

  private[engine] def retrieveNodeData(bb: BoundingBox, tag: Tag)(implicit ec: ExecutionContext): Future[List[OsmDenormalizedObject]] = retrieveData(OsmTypeNode)(bb, tag)(ec)

  private[engine] def retrieveWayData(bb: BoundingBox, tag: Tag)(implicit ec: ExecutionContext): Future[List[OsmDenormalizedObject]] = retrieveData(OsmTypeWay)(bb, tag)(ec)

  private[engine] def retrieveRelationData(bb: BoundingBox, tag: Tag)(implicit ec: ExecutionContext): Future[List[OsmDenormalizedObject]] = retrieveData(OsmTypeRelation)(bb, tag)(ec)

  private[engine] def retrieveData(typ: OsmType)(bb: BoundingBox, tag: Tag)(implicit ec: ExecutionContext): Future[List[OsmDenormalizedObject]] = {
    val storageService = OsmStorageService()
    storageService.findBBTag(bb.hash, tag.tag, typ).map(_.map((list) => list.element))
  }


  private[engine] def retrieveNodeById(bb: BoundingBox, id: Id)(implicit ec: ExecutionContext) = retrieveDataById(OsmTypeNode)(bb, id)(ec)

  private[engine] def retrieveWayById(bb: BoundingBox, id: Id)(implicit ec: ExecutionContext) = retrieveDataById(OsmTypeWay)(bb, id)(ec)

  private[engine] def retrieveRelationById(bb: BoundingBox, id: Id)(implicit ec: ExecutionContext) = retrieveDataById(OsmTypeRelation)(bb, id)(ec)

  private[engine] def retrieveDataById(typ: OsmType)(bb: BoundingBox, id: Id)(implicit ec: ExecutionContext): Future[List[OsmDenormalizedObject]] = {
    val storageService = OsmStorageService()
    storageService
      .findBB(bb.hash, id.id, typ)
      .map(_.map(_.element).toList)
  }


  private[engine] def retrieveNodeId(name: Name)(implicit ec: ExecutionContext) = retrieveId(OsmTypeNode)(name)(ec)

  private[engine] def retrieveWayId(name: Name)(implicit ec: ExecutionContext) = retrieveId(OsmTypeWay)(name)(ec)

  private[engine] def retrieveRelationId(name: Name)(implicit ec: ExecutionContext) = retrieveId(OsmTypeRelation)(name)(ec)

  private[engine] def retrieveId(typ: OsmType)(name: Name)(implicit ec: ExecutionContext): Future[List[Id]] = {
    val indexService = IndexingService()
    import scalaz.syntax.id._
    indexService.queryForOsmObject(name.name, typ).map(_.map(_.id |> Id))
  }

  private[engine] def retrieveNodeBB(id: Id)(implicit ec: ExecutionContext) = retrieveBB(OsmTypeNode)(id)(ec)

  private[engine] def retrieveWayBB(id: Id)(implicit ec: ExecutionContext) = retrieveBB(OsmTypeWay)(id)(ec)

  private[engine] def retrieveRelationBB(id: Id)(implicit ec: ExecutionContext) = retrieveBB(OsmTypeRelation)(id)(ec)

  private[engine] def retrieveBB(typ: OsmType)(id: Id)(implicit ec: ExecutionContext): Future[List[BoundingBox]] = {
    val mappingService = MappingService()
    mappingService.findMapping(id.id, typ)
      .map(
        x => {
          x.map(
            (mapping) => BoundingBox(mapping.hash)).toList
        }
      )
  }


  def location(lon: Double, lat: Double): Source[Location, Unit] = Source.single(Location(Point(lon, lat)))

}
