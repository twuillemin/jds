package net.wuillemin.jds.dataserver.entity.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import net.wuillemin.jds.common.entity.Loggable

/**
 * The base schema definition that all schema must implements
 *
 * @param id The id of the schema
 * @param name The name of the schema
 * @param serverId The id of the server of the schema
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = SchemaSQL::class, name = "SQL"),
    JsonSubTypes.Type(value = SchemaGSheet::class, name = "GSheet"))
sealed class Schema(
    open val id: String?,
    open val name: String,
    open val serverId: String
) : Loggable {

    override fun getLoggingId(): String {
        return "'${this.name} [id: ${this.id}]'"
    }
}


/**
 * A SQL schema
 *
 * @param roleName The name of the role. If given, this will be used, to limit the session access for each connection
 * created against the schema
 */
data class SchemaSQL(
    // Inherited properties
    override val id: String?,
    override val name: String,
    override val serverId: String,
    // Specific properties
    val roleName: String?
) : Schema(id, name, serverId)


/**
 * A GSheet Schema, which is a worksheet
 */
data class SchemaGSheet(
    // Inherited properties
    override val id: String?,
    override val name: String,
    override val serverId: String
) : Schema(id, name, serverId)
