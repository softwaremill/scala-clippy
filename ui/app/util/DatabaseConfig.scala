package util

import com.typesafe.config.Config

trait DatabaseConfig extends ConfigWithDefault {
  def rootConfig: Config

  import DatabaseConfig._

  lazy val dbH2Url              = getString(s"db.h2.properties.url", "jdbc:h2:file:./data")
  lazy val dbPostgresServerName = getString(PostgresServerNameKey, "")
  lazy val dbPostgresPort       = getString(PostgresPortKey, "5432")
  lazy val dbPostgresDbName     = getString(PostgresDbNameKey, "")
  lazy val dbPostgresUsername   = getString(PostgresUsernameKey, "")
  lazy val dbPostgresPassword   = getString(PostgresPasswordKey, "")
}

object DatabaseConfig {
  val PostgresDSClass       = "db.postgres.dataSourceClass"
  val PostgresServerNameKey = "db.postgres.properties.serverName"
  val PostgresPortKey       = "db.postgres.properties.portNumber"
  val PostgresDbNameKey     = "db.postgres.properties.databaseName"
  val PostgresUsernameKey   = "db.postgres.properties.user"
  val PostgresPasswordKey   = "db.postgres.properties.password"
}
