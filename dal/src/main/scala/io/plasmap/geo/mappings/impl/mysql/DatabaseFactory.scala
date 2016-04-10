package io.plasmap.geo.mappings.impl.mysql

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.mysql.MySQLConnection
import com.github.mauricio.async.db.mysql.pool.MySQLConnectionFactory
import com.github.mauricio.async.db.pool.{ConnectionPool, PoolConfiguration}
import com.typesafe.scalalogging.Logger
import io.plasmap.geo.mappings.impl.mysql.impl.MySQLDatabaseImpl
import org.slf4j.LoggerFactory

object DatabaseFactory {

  def newDatabase(
                   host: String, port: Int,
                   username: String,
                   password: Option[String] = None,
                   database: Option[String] = None,
                   dbPoolMaxObjects: Int = 32,
                   dbPoolMaxIdle: Int = 360,
                   dbPoolMaxQueueSize: Int = 5000
                   ): MySQLDatabase = {

    val configuration = new Configuration(
      host = host,
      username = username,
      port = port,
      password = password,
      database = database)

    val factory = new MySQLConnectionFactory(configuration)

    val pool: ConnectionPool[MySQLConnection] = new ConnectionPool(factory, new PoolConfiguration(dbPoolMaxObjects, dbPoolMaxIdle, dbPoolMaxQueueSize))


    val log = Logger(LoggerFactory.getLogger(DatabaseFactory.getClass.getName))
    log.info(s"${Console.GREEN}Created MySQL connection pool with config [dbPoolMaxObjects=$dbPoolMaxObjects,dbPoolMaxIdle=$dbPoolMaxIdle,dbPoolMaxQueueSize=$dbPoolMaxQueueSize]${Console.RESET}")

    MySQLDatabaseImpl(pool)
  }

}