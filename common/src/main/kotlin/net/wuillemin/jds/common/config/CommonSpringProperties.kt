package net.wuillemin.jds.common.config

import net.wuillemin.jds.common.config.CommonProperties.BasicAuthentication
import net.wuillemin.jds.common.config.CommonProperties.Client
import net.wuillemin.jds.common.config.CommonProperties.JWTAuthentication
import net.wuillemin.jds.common.config.CommonProperties.NoAuthentication
import net.wuillemin.jds.common.config.CommonProperties.Server
import net.wuillemin.jds.common.security.ServerReference
import net.wuillemin.jds.common.security.utils.CertificateFileReader
import org.slf4j.Logger
import org.springframework.beans.factory.BeanCreationException
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI
import java.nio.file.Paths


/**
 * Main class for the configuration of the Base services
 *
 * @param certificateFileReader The service for reading certificates files
 * @param logger The logger
 */
@Configuration
@ConfigurationProperties(CommonSpringProperties.CONTEXT)
class CommonSpringProperties(
    private val certificateFileReader: CertificateFileReader,
    private val logger: Logger) {

    // Declare class constants
    companion object {
        /**
         * Define the context of the settings for authentication
         */
        const val CONTEXT: String = "jds.common"
    }

    /**
     * Construct a bean having all the properties verified and ready to be used
     *
     * @return an CommonProperties bean with all properties validated
     */
    @Bean
    fun buildCommonProperties(): CommonProperties {

        // Get the public key
        val publicKey = server?.publicKeyPath?.let { publicKeyPath ->

            if (publicKeyPath.isNotBlank()) {
                // Get the private key
                try {
                    certificateFileReader.readDERPublicKey(Paths.get(publicKeyPath))
                }
                catch (e: Exception) {
                    throw BeanCreationException("Unable to read the private key", e)
                }
            }
            else {
                null
            }
        }

        // Warn if no public key given
        if (publicKey == null) {
            logger.warn("No Public key given. Application will run in non secured mode.")
        }

        // Get the server to connect to with JWT
        val jwtAuthentication = client
            ?.jwt
            ?.asSequence()
            ?.filter { it.authenticationServerURI != null && it.userName != null && it.password != null && it.servers != null }
            ?.map { property ->
                JWTAuthentication(
                    URI.create(property.authenticationServerURI),
                    property.userName!!,
                    property.password!!,
                    property.servers!!.asSequence().filterNotNull().map { getServerDetails(it) }.toList())
            }
            ?.toList()
            ?: emptyList()

        // Get the server to connect to with Basic Auth
        val basicAuthentication = client
            ?.basic
            ?.asSequence()
            ?.filter { it.userName != null && it.password != null && it.servers != null }
            ?.map { property ->
                BasicAuthentication(
                    property.userName!!,
                    property.password!!,
                    property.servers!!.asSequence().filterNotNull().map { getServerDetails(it) }.toList())
            }
            ?.toList()
            ?: emptyList()

        // Get the server to connect to with no Auth
        val noAuthentication = NoAuthentication(
            client?.noAuth
                ?.let { no ->
                    no.servers
                        ?.asSequence()?.filterNotNull()?.map { getServerDetails(it) }?.toList()
                        ?: emptyList()
                }
                ?: emptyList())

        return CommonProperties(
            database,
            Server(publicKey),
            Client(jwtAuthentication, basicAuthentication, noAuthentication))
    }


    /**
     * The definition of the common database
     */
    var database: MongoProperties = MongoProperties()

    /**
     * The path of to the file holding the private key
     */
    var server: ServerProperties? = null

    /**
     * The various authentication
     */
    var client: ClientProperties? = null

    /**
     * function to convert the string to an host and port pair
     *
     * @param server The server string coming from the property file
     * @return a ServerReference object
     */
    private fun getServerDetails(server: String): ServerReference {

        val uri = URI.create(server)
        return ServerReference(uri.host, uri.port)
    }

    /**
     * Inner class grouping together the properties for the application acting as a server
     */
    class ServerProperties {

        /**
         * The path of to the file holding the private key
         */
        var publicKeyPath: String? = null
    }

    /**
     * Inner class grouping together the properties for the application acting as a client
     */
    class ClientProperties {

        /**
         * The list of servers requiring JWT authentication
         */
        var jwt: List<JwtAuthenticationSpringProperties>? = null

        /**
         * The list of servers requiring Basic authentication
         */
        var basic: List<BasicAuthenticationSpringProperties>? = null

        /**
         * The list of servers requiring No authentication
         */
        var noAuth: NoAuthenticationSpringProperties? = null
    }

    /**
     * The definition of a server requiring JWT authentication
     */
    class JwtAuthenticationSpringProperties {

        /**
         * The server generating the token
         */
        var authenticationServerURI: String? = null

        /**
         * The name of the user to connect with
         */
        var userName: String? = null

        /**
         * The name of the user to connect with
         */
        var password: String? = null

        /**
         * The list of servers to connect to
         */
        var servers: List<String?>? = null
    }

    /**
     * The definition of a server requiring Basic authentication
     */
    class BasicAuthenticationSpringProperties {
        /**
         * The name of the user to connect with
         */
        var userName: String? = null

        /**
         * The name of the user to connect with
         */
        var password: String? = null

        /**
         * The list of servers to connect to
         */
        var servers: List<String?>? = null
    }

    /**
     * The definition of a server requiring No authentication
     */
    class NoAuthenticationSpringProperties {
        /**
         * The list of servers to connect to
         */
        var servers: List<String?>? = null
    }
}