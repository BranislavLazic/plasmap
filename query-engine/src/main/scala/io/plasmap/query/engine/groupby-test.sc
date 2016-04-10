import scala.util.Random


val nOriginal: Int = 1234567
val strings = Seq.fill(nOriginal)(Random.nextString(5))
val hashes = strings
  .map(_.hashCode())

val k: Int = 65536

val sortedHashes:List[Double] =
  hashes
    .map( (x) => (x:Double) / Int.MaxValue)
    .map(Math.abs)
    .sorted
    .take(k)
    .toList

val nExpected = (sortedHashes.size - 1) / sortedHashes.last