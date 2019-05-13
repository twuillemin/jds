package net.wuillemin.jds.dataserver.app.monolith

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.runApplication

/**
 * Main class of the whole application
 */
@SpringBootApplication(
    scanBasePackages = ["net.wuillemin.jds.dataserver.app.monolith.config"],
    exclude = [MongoAutoConfiguration::class, MongoDataAutoConfiguration::class])
class MonolithApplication

/**
 * Entry point of the program
 */
fun main(args: Array<String>) {
    runApplication<MonolithApplication>(*args)
}
