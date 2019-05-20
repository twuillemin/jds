package net.wuillemin.jds.dataserver.config

import org.springframework.beans.factory.BeanCreationException
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


/**
 * Main class for the configuration of the Base services
 */
@Configuration
@ConfigurationProperties(DataServerSpringProperties.CONTEXT)
class DataServerSpringProperties {

    // Declare class constants
    companion object {
        /**
         * Define the context of the settings for the data server
         */
        const val CONTEXT: String = "jds.dataserver"
    }

    /**
     * Construct a bean having all the properties verified and ready to be used
     *
     * @return an DataServerProperties bean with all properties validated
     */
    @Bean
    fun buildDataServerProperties(): DataServerProperties {

        // Valid the base attributes
        val configuration = checkAndConvertDatabaseDefinition(configurationDatabase)

        // Valid the base attributes
        val storage = storageDatabases.map { checkAndConvertDatabaseDefinition(it) }

        if (storage.isEmpty()) {
            throw BeanCreationException("There must be at least a database defined in 'storageDatabases'")
        }

        return DataServerProperties(configuration, storage)
    }

    /**
     * The definition of the database for storing the configuration of the model
     */
    var configurationDatabase: SQLDatabaseProperties? = null

    /**
     * The definition of the database for storing the configuration of the model
     */
    var storageDatabases: MutableList<SQLDatabaseProperties> = ArrayList()

    /**
     * The configuration for a SQL database
     */
    class SQLDatabaseProperties {

        /**
         * The JDBC connection string
         */
        var url: String? = null

        /**
         * The user
         */
        var user: String? = null

        /**
         * The password
         */
        var password: String? = null

        /**
         * The driver
         */
        var driverClassName: String? = null
    }

    private fun checkAndConvertDatabaseDefinition(db: SQLDatabaseProperties?): DataServerProperties.SQLDatabaseProperties {

        db ?: throw BeanCreationException("The property 'commonDatabase' of the database must not be blank")

        val url = db.url
        if (url.isNullOrBlank())
            throw BeanCreationException("The property 'url' of the database must not be blank")

        val driverClassName = db.driverClassName
        if (driverClassName.isNullOrBlank())
            throw BeanCreationException("The property 'driverClassName' of the database must not be blank")

        return DataServerProperties.SQLDatabaseProperties(
            url,
            db.user,
            db.password,
            driverClassName
        )
    }
}