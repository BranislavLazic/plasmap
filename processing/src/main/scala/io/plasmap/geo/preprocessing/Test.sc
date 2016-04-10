val list = List(1,2,3,4,5,1,2,3,4,5,6)

def check(x:Any) = true

list
  .map(_.toString)
  .foldLeft( Set.empty[String]) ( (acc: Set[String], x) => {

    if (check(x)) {
      acc + x
    } else {
      acc
    }
  } )

val words = List( "jan", "jan", "philipp", "philipp", "philipp","marius")

type T = (String, Int)
val zero: List[T] = List.empty[T]
val foldFunc: (List[T], T) => List[T] = (acc, tuple) => {
  tuple :: acc
}

words
  .map( (word) => word -> word.length )
  .foldLeft(zero)( foldFunc)

val userLookup = ("jan" -> 1, "philipp" -> 2)

val newUsers = List("jan", "marius", "peter", "philipp")

type ACC1 =

type ACC = (List[(String,Int)],Set[String])
val zeroN:ACC = List.empty[(String,Int)] -> Set.empty[String]

def foldF(acc:ACC, element:String):ACC = {
  val (oldList,oldSet) = acc
  val newList = element -> element.length
  val newSet = element + element

  newList -> newSet
}

newUsers.fold(zeroN)(foldF)

