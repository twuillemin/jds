package net.wuillemin.jds.dataserver.entity.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import net.wuillemin.jds.common.entity.Loggable

/**
 * The base Server definition
 *
 * @param id The id of the server.
 * @param name The name of the server.
 * @param groupId The id of the group owning the server
 * @param customerDefined If the server is managed by JDS (and so should stay hidden)
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ServerSQL::class, name = "SQL"),
    JsonSubTypes.Type(value = ServerGSheet::class, name = "GSheet")
)
sealed class Server(
    open val id: Long?,
    open val name: String,
    open val groupId: Long,
    open val customerDefined: Boolean
) : Loggable {

    override fun getLoggingId(): String {
        return "'${this.name} [id: ${this.id}]'"
    }

    /**
     * Return a new server object with all the sensitive information removed
     *
     * @return a server without any sensible information
     */
    abstract fun copyWithoutSensitiveInformation(): Server
}

/**
 * Represents a SQL server
 *
 * @param jdbcURL The JDBC connection string
 * @param userName The user name to connect
 * @param password The password to connect
 * @param driverClassName The driver of the connection
 */
data class ServerSQL(
    // Inherited properties
    override val id: Long?,
    override val name: String,
    override val groupId: Long,
    override val customerDefined: Boolean,
    // Specific properties
    val jdbcURL: String,
    val userName: String?,
    val password: String?,
    val driverClassName: String
) : Server(id, name, groupId, customerDefined) {

    override fun copyWithoutSensitiveInformation(): Server {
        return copy(
            jdbcURL = "*****",
            userName = "*****",
            password = "*****"
        )
    }
}

/**
 * Represents a Postgres server
 *
 * @param userName The name of the user
 * @param password The password of the user
 * @param workbookURL The link to the resource
 */
data class ServerGSheet(
    // Inherited properties
    override val id: Long?,
    override val name: String,
    override val groupId: Long,
    override val customerDefined: Boolean,
    // Specific properties
    val workbookURL: String,
    val userName: String,
    val password: String
) : Server(id, name, groupId, customerDefined) {

    override fun copyWithoutSensitiveInformation(): Server {
        return copy(
            userName = "*****",
            password = "*****",
            workbookURL = "*****"
        )
    }
}