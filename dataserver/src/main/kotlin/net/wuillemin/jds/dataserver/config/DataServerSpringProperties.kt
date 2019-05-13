package net.wuillemin.jds.dataserver.config

import org.springframework.beans.factory.BeanCreationException
import org.springframework.boot.autoconfigure.mongo.MongoProperties
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
        val storage = storageDatabases.map { database ->

            database.jdbcConnectionUrl
                ?.let { jdbcConnectionUrl ->
                    if (jdbcConnectionUrl.isBlank()) {
                        throw BeanCreationException("The property 'jdbcConnectionUrl' must not be blank")
                    }
                    DataServerProperties.SQLDatabaseProperties(
                        jdbcConnectionUrl,
                        database.user,
                        database.password
                    )
                }
                ?: throw BeanCreationException("The property 'jdbcConnectionUrl' must not be null")
        }

        if (storage.isEmpty()) {
            throw BeanCreationException("There must be at least a database defined in 'storageDatabases'")
        }

        return DataServerProperties(configurationDatabase, storage)
    }

    /**
     * The definition of the database for storing the configuration of the model
     */
    var configurationDatabase: MongoProperties = MongoProperties()

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
        var jdbcConnectionUrl: String? = null

        /**
         * The user
         */
        var user: String? = null

        /**
         * The password
         */
        var password: String? = null
    }
}