import io.plasmap.queryengine.macros.Macros.GeneratePOIQueries
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import scala.language.experimental.macros

/**
  * Created by mark on 27.10.15.
  */
object Model {
  sealed trait A
  case class B(x:Int)      extends A
  case class C(x:String)   extends A
  case class Zed(x:Double) extends A
}

object Mocks {
  sealed trait SourceGraph[A]
  sealed trait POIQuery[A]
}

object Generated {
  import Mocks.POIQuery
  import Mocks.SourceGraph
  @GeneratePOIQueries[Model.A]("POIQuery") case object D
}

object GeneratePOIQueriesMacroSpec extends Specification {
  "The poi query generation macro" should {

    "generate case classes in an annotated object" in {
      import Model._
      import Generated._
      import Mocks._
      D.POIQueryB(new SourceGraph[B]{}) must not beNull
    }

    "generate all case classes in an annotated object" in {
      import Model._
      import Generated._
      import Mocks._
      val b = D.POIQueryB  (new SourceGraph[B]  {})
      val c = D.POIQueryC  (new SourceGraph[C]  {})
      val z = D.POIQueryZed(new SourceGraph[Zed]{})
      List(b,c,z).filter(_ != null) must haveSize(3)
    }
  }
}
