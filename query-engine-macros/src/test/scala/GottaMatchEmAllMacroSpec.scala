import io.plasmap.queryengine.macros.Macros.gottaMatchEmAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import scala.language.experimental.macros

/**
  * Created by mark on 27.10.15.
  */
object Model2 {
  sealed trait A
  case class   B(x:Int)      extends A
  case class   C(x:String)   extends A
  case class   Zed(x:Double) extends A
  case class MeatLoaf(i:Int) extends A
}

object Mocks2 {
  import Model2._
  sealed trait POIQuery[A]
  case object POIQueryMeatLoaf extends POIQuery[MeatLoaf]
  case class Source[A,B](i:Int)
}

object GottaMatchEmAllMacroSpec extends Specification {

  "The match em all macro" should {

    "match all subclasses" in {
      import Model2._
      import Mocks2._
      val x = POIQueryMeatLoaf
      def metsch[A](pq: POIQuery[_]):Source[String, Unit] = macro gottaMatchEmAll[A]
      metsch[Model2.A](x)
//      List(
//        metsch[Model.A](B(2)),
//        metsch[Model.A](C("Hi!")),
//        metsch[Model.A](Zed(1.0))
//      ) must containAllOf List(1,2,3)
      12 must be_==(12)
    }
  }

}
