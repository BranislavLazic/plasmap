package io.plasmap.query.engine


case class Boxed[+E](element:E, current:(Int,Long), previous:Map[Int,Long])
