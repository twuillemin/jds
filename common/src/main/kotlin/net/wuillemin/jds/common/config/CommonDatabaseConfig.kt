package net.wuillemin.jds.common.config

import com.mongodb.MongoClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoDbFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoDbFactory
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories


/**
 * Configuration for the common database used to store user and password
 */
@Configuration
@EnableMongoRepositories(
    basePackages = ["net.wuillemin.jds.common.repository"],
    mongoTemplateRef = "commonMongoTemplate")
class CommonDatabaseConfig(private val commonProperties: CommonProperties) {

    /**
     * The mongo template to the common database
     */
    @Bean(name = ["commonMongoTemplate"])
    fun commonMongoTemplate(): MongoTemplate {
        return MongoTemplate(commonMongoFactory())
    }

    /**
     * The mongo connection to the common database
     */
    @Bean(name = ["commonMongoFactory"])
    fun commonMongoFactory(): MongoDbFactory {
        return SimpleMongoDbFactory(
            MongoClient(
                commonProperties.database.host,
                commonProperties.database.port),
            commonProperties.database.database)
    }
}
