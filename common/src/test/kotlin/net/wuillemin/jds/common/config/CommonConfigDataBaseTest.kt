package net.wuillemin.jds.common.config

import com.mongodb.MongoClient
import com.mongodb.ServerAddress
import de.bwaldvogel.mongo.H2MongoServer
import de.bwaldvogel.mongo.MongoServer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.MongoDbFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoDbFactory
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories


@EnableMongoRepositories(
    basePackages = ["net.wuillemin.jds.common.repository"],
    mongoTemplateRef = "commonMongoTemplate")
@Import(CommonConfigTest::class)
class CommonConfigDataBaseTest {

    @Bean(name = ["commonMongoTemplate"])
    fun mongoTemplate(mongoClient: MongoClient): MongoTemplate {
        return MongoTemplate(mongoDbFactory(mongoClient))
    }

    @Bean(name = ["commonMongoFactory"])
    fun mongoDbFactory(mongoClient: MongoClient): MongoDbFactory {
        return SimpleMongoDbFactory(mongoClient, "test")
    }

    @Bean(destroyMethod = "shutdown")
    fun mongoServer(): MongoServer {
        val mongoServer = H2MongoServer()
        mongoServer.bind()
        return mongoServer
    }

    @Bean(destroyMethod = "close")
    fun mongoClient(mongoServer: MongoServer): MongoClient {
        return MongoClient(ServerAddress(mongoServer.localAddress))
    }
}