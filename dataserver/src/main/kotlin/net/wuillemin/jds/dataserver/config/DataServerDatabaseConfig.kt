package net.wuillemin.jds.dataserver.config

import com.mongodb.MongoClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoDbFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoDbFactory
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories


/**
 * Configuration for the authentication database used to store user and password
 */
@Configuration
@EnableMongoRepositories(
    basePackages = ["net.wuillemin.jds.dataserver.repository"],
    mongoTemplateRef = "dataServerMongoTemplate")
class DataServerDatabaseConfig(private val dataServerProperties: DataServerProperties) {

    /**
     * The mongo template to the authentication database
     */
    @Bean(name = ["dataServerMongoTemplate"])
    fun dataServerMongoTemplate(): MongoTemplate {
        return MongoTemplate(dataServerMongoFactory())
    }

    /**
     * The mongo connection to the authentication database
     */
    @Bean(name = ["dataServerMongoFactory"])
    fun dataServerMongoFactory(): MongoDbFactory {
        return SimpleMongoDbFactory(
            MongoClient(
                dataServerProperties.configurationDatabase.host,
                dataServerProperties.configurationDatabase.port),
            dataServerProperties.configurationDatabase.database)
    }
}
