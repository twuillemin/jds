package net.wuillemin.jds.dataserver.config

/**
 * Main class for the configuration of the data server
 *
 * @param configurationDatabase The database for configuration
 * @param storageDatabase The database for storage
 */
data class DataServerProperties(
    val configurationDatabase: SQLDatabaseProperties,
    val storageDatabase: List<SQLDatabaseProperties>
) {


    /**
     * The configuration for a SQL database
     *
     * @param jdbcConnectionUrl The JDBC connection string
     * @param user The user
     * @param password The password
     * @param driverClassName The name of the driver
     */
    data class SQLDatabaseProperties(
        val jdbcConnectionUrl: String,
        val user: String?,
        val password: String?,
        val driverClassName: String
    )
}
