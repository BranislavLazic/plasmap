package io.plasmap.driver.elasticsearch
import com.sksamuel.elastic4s.ElasticClient

/**
 * Created by mark on 10.02.15.
 */
object Indexer {
  def apply(host:String, port:Int):ElasticClient = ElasticClient.remote(host, port)
}

object LocalIndexer {
  def apply():ElasticClient = ElasticClient.local
}
