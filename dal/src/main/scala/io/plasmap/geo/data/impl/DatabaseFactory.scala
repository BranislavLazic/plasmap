package io.plasmap.geo.data.impl

import com.datastax.driver.core.{ProtocolOptions, Cluster}

object DatabaseFactory {

  def newDatabase(
                   hosts: List[String],
                   port: Int = 9042,
                   username: String,
                   password: Option[String] = None
                   ): Cluster = {

      Cluster.builder().
        addContactPoints(hosts: _*).
        withPort(port).
        withCompression(ProtocolOptions.Compression.SNAPPY).
        build()
  }

}