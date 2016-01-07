package util

import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.jdbc.JdbcBackend._

case class SqlDatabase(db: slick.jdbc.JdbcBackend#Database, driver: JdbcProfile) {
  def close() {
    db.close()
  }
}

object SqlDatabase {
  def createH2(connectionString: String): SqlDatabase = {
    val db = Database.forURL(connectionString)
    new SqlDatabase(db, slick.driver.H2Driver)
  }

  def fromConfig(cfg: DatabaseConfig[JdbcProfile]) = SqlDatabase(cfg.db, cfg.driver)
}
