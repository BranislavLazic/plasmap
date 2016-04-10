package io.plasmap.geo.mappings.impl.mysql.impl

import com.github.mauricio.async.db.{RowData, QueryResult}
import com.github.mauricio.async.db.mysql.MySQLConnection
import com.github.mauricio.async.db.pool.ConnectionPool
import io.plasmap.geo.mappings.impl.mysql.MySQLDatabase

import scala.concurrent.{Future, ExecutionContext}

case class MySQLDatabaseImpl(pool:ConnectionPool[MySQLConnection]) extends MySQLDatabase {
  /**
   * Creates a prepared statement with the given query
   * and passes it to the connection pool with given values.
   */
  override def execute(query: String, values: Any*)(implicit ec: ExecutionContext): Future[QueryResult] =  {
    if (values.size > 0)
      pool.sendPreparedStatement(query, values)
    else
      pool.sendQuery(query)
  }

  /**
   * Creates a prepared statement with the given query
   * and passes it to the connection pool with given values.
    *
    * @return Seq[RowData] of the query
   */
  override def fetch(query: String, values: Any*)(implicit ec: ExecutionContext): Future[Option[Seq[RowData]]] =
    execute(query, values: _*).map(_.rows)
}
