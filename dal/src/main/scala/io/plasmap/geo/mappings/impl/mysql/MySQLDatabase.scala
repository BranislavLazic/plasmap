package io.plasmap.geo.mappings.impl.mysql
import com.github.mauricio.async.db.{QueryResult, RowData}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by JansonHanson on 16.01.2015.
 */
trait MySQLDatabase {

  /**
   * Creates a prepared statement with the given query
   * and passes it to the connection pool with given values.
   */
  def execute(query: String, values: Any*)(implicit ec: ExecutionContext): Future[QueryResult]

  /**
   * Creates a prepared statement with the given query
   * and passes it to the connection pool with given values.
   * @return Seq[RowData] of the query
   */
  def fetch(query: String, values: Any*)(implicit ec: ExecutionContext): Future[Option[Seq[RowData]]]
}
