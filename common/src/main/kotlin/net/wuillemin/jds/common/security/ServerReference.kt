package net.wuillemin.jds.common.security

/**
 * Define how a server is seen by the network. Currently only the host and the port define a server as
 * it is enough to fill the needs
 *
 * @param host The host of the server
 * @param port The port of the server
 */
data class ServerReference(
    val host: String,
    val port: Int
)