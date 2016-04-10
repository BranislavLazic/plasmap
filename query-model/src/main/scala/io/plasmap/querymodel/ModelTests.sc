import boopickle.Default._
import io.plasmap.querymodel.{PMCityFromName, PMRestaurantsFromArea}
import io.plasmap.querymodel._
import PMQuery._

val q = PMCityFromName("italy")
val l = Pickle.intoBytes(q)
