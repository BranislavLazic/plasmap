import io.plasmap.geohash.Precision

/*
val k = 4096
val u = 5000000
val distinct = 5000

val list = Seq.fill(u)(Math.abs(Random.nextInt(distinct)))
val sketch = Sketches.updateSketchBuilder().
  /*setResizeFactor(ResizeFactor.X1).
  setFamily(Family.ALPHA).*/build(k )
for (i <- 0 to 10000) {
  val elem: Int = list(i)
  sketch.update(elem)
  //if(i % 100 == 0) println(sketch.getEstimate)
}
println(sketch.getEstimate.toInt)*/


val x : Either[String,Int] = Right(10)

x.left.map()