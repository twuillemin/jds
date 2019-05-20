package net.wuillemin.jds.common.config

import net.wuillemin.jds.common.security.ServerReference
import java.net.URI
import java.security.PublicKey

/**
 * Main class for the configuration of the common properties
 *
 * @param database The common database
 * @param server Configuration of the network when acting as a server
 * @param client Configuration of the network when acting as a client
 */
data class CommonProperties(
    val database: SQLDatabaseProperties,
    val server: Server,
    val client: Client
) {

    /**
     * Configuration of the network when acting as a server
     *
     * @param publicKey The public key to validate the tokens
     */
    data class Server(
        val publicKey: PublicKey?
    )

    /**
     * Configuration of the network when acting as a client
     *
     * @param jwtAuthentications The servers with JWTAuthentication
     * @param basicAuthentications The servers with BasicAuthentication
     * @param noAuthentications The servers with NoAuthentication
     */
    data class Client(
        val jwtAuthentications: List<JWTAuthentication>,
        val basicAuthentications: List<BasicAuthentication>,
        val noAuthentications: NoAuthentication
    )

    /**
     * The configuration of the connections with JWT
     *
     * @param authenticationServerURI The URI of the server generating the token
     * @param userName The name of the user to connect with
     * @param password The name of the user to connect with
     * @param targets The list of servers to connect to
     */
    data class JWTAuthentication(
        val authenticationServerURI: URI,
        val userName: String,
        val password: String,
        val targets: List<ServerReference>
    )

    /**
     * The configuration of the connections with Basic authentication
     *
     * @param userName The name of the user to connect with
     * @param password The name of the user to connect with
     * @param targets The list of servers to connect to
     */
    data class BasicAuthentication(
        val userName: String,
        val password: String,
        val targets: List<ServerReference>
    )

    /**
     * The configuration of the connections without any authentication
     *
     * @param targets The list of servers to connect to
     */
    data class NoAuthentication(
        val targets: List<ServerReference>
    )

    /**
     * The configuration for a SQL database
     *
     * @param jdbcConnectionUrl The JDBC connection string
     * @param user The user
     * @param password The password
     * @param driverClassName The driver
     */
    data class SQLDatabaseProperties(
        val jdbcConnectionUrl: String,
        val user: String?,
        val password: String?,
        val driverClassName: String
    )
}
