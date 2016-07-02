package io.plasmap.queryengine.macros

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros

import scala.reflect.macros.blackbox
import scala.reflect.macros.whitebox

/**
 * Created by mark on 13.10.15.
 */

object Macros {
  class GeneratePOIQueries[A](prefix:String) extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro GeneratePOIQueries.theMacro
  }

  object GeneratePOIQueries {
    def theMacro(c: whitebox.Context)(annottees:c.Expr[Any]*): c.Expr[Any] = {
      import c.universe._
      //Please keep for debugging purposes
      def abort(msg:String) = c.abort(c.enclosingPosition, msg)
      def fail(msg:String) = abort(s"\n----------------\nFailing:\n\n$msg\n\n----------------")
      def echo(msg:String) = c.echo(c.enclosingPosition, msg)
      def log(msg:String) = echo(s"\n----------------\nLogging:\n\n$msg\n\n----------------")

      //Get the type argument and the regular argument
      val (typeArgAny:c.Tree, prefixRaw:c.Tree) = c.macroApplication match {
        case q"new GeneratePOIQueries[$t]($prefix).macroTransform( ..$rest )" => (t, prefix)
        case _ => abort("Missing type argument or prefix argument. Should be something like: GeneratePOIQueries[A](\"Prefix\"")
      }

      //Strip surrounding "s
      val prefix = prefixRaw.toString.tail.dropRight(1)

      val toCheck = c.Expr[Any](q"$typeArgAny")
      val theTrait = c.typeCheck(q"??? : $toCheck").tpe.typeSymbol.asClass

      if(!theTrait.isSealed || !theTrait.isTrait)
        abort("Type argument must be a sealed trait")

      val objName = annottees.map(_.tree).head match {
        case q"object $name extends ..$extended"      => name
        case q"case object $name extends ..$extended" => name
        case _ => abort("Annotate an object please")
      }

      theTrait.typeSignature //Workaround SI-7046
      val subclasses = theTrait.knownDirectSubclasses
      require(subclasses.nonEmpty, s"Could not find any direct subclasses to the given trait $theTrait")
      val subclassTrees = subclasses.map(sc => {
         val name = TypeName(prefix + sc.name.toString)
         val extended = TypeName(prefix)
         val B = sc.asType
         q"final case class $name(shape:SourceGraph[$B]) extends $extended[$B]"
      })

      val obj = subclassTrees.foldLeft(c.Expr[Any](q"object $objName { }")){
        case (acc:c.Expr[Any], u:c.Tree) =>
          acc.tree match {
            case q"object $n { ..$b }" =>
              val oneMore = u :: b
              c.Expr[Any](q"object $n { ..$oneMore }")
          }
        case _ => abort("Fucking hell, can't you get anything right?")
      }
      echo("\nMacro is building POIQuerys for:\n" + subclasses.map(_.toString.drop(6)))
      obj
    }
  }

  def gottaMatchEmAll[A : c.WeakTypeTag](c:blackbox.Context)(pq:c.Tree): c.Tree = {
    import c.universe._
    def abort(msg:String) = c.abort(c.enclosingPosition, msg)
    //Please keep for debugging purposes
    def fail(msg:String) = abort(s"\n----------------\nFailing:\n\n$msg\n\n----------------")

    val tA         = c.weakTypeOf[A]
    c.typecheck(q"type T = $tA")
    val subclasses = tA.typeSymbol.asClass.knownDirectSubclasses
    val subclassTrees = subclasses.map(sc => {
      val name = TypeName("POIQuery" + sc.name.toString)
      val typ = sc.asType.name
      q"""
        $pq match {
        case x:$name =>
            val poi = implicitly[POI[$typ]]
            val serialise: ($typ) => String = (s:$typ) => OsmDenormalizedSerializer.toGeoJsonString(poi.osmObj(s))
            val source: Source[String, NotUsed] = Source.fromGraph(x.shape).via(serialiser(serialise))
            source
        }
      """
    })

    val method = subclassTrees.foldLeft(c.Expr[Any](q"")){
      case (acc:c.Expr[Any], u:c.Tree) =>
        u match {
          case q"$pq match { case $content }" =>
            val res = acc.tree match {
              case q"" /* only the first */      => content :: Nil
              case q"$pq match { case ..$prev }" => content :: prev
            }
            c.Expr[Any](q"$pq match { case ..$res }")
        }
      }
    method.tree
  }

  def poiTypeClassInstanceMacro[A : c.WeakTypeTag](c:blackbox.Context)(tagKey:c.Tree, tagVal:c.Tree): c.Tree = {
    import c.universe._
    val tA         = c.weakTypeOf[A]
    val ACompanion = tA.typeSymbol.companion
    val n = "POIQuery"+tA.toString.split("\\.").last
    val name       = TermName(n)
    val typeName   = TypeName(n)
    c.typecheck(q"type T = $typeName")
    q"""
      new POI[$tA] {
        override def tags: List[OsmTag] = List(OsmTag($tagKey,$tagVal))
        override def osmObj(poi: $tA):OsmDenormalizedObject = poi.osmObject
        override def bbsToQuery(rel: OsmDenormalizedRelation): List[(BoundingBox, Tag)] =
          bbsToQueryFromTag(rel, $tagKey, $tagVal)
        override def queryFromShape(shape: SourceGraph[$tA]):POIQuery[$tA] =
          $name(shape)
        override def fromOsmDenObj(oo: OsmDenormalizedObject): $tA =
          $ACompanion(oo)
        val name = $tagVal
      }
    """
  }
}
