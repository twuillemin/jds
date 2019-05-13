package net.wuillemin.jds.dataserver.entity.model

import net.wuillemin.jds.common.entity.Loggable

/**
 * Represents a Postgres server
 *
 * @param id The id of the DataSource
 * @param dataProviderId The id of the DataProvider
 * @param name The name of the DataSource
 * @param userAllowedToReadIds The list of ids of the users who can read objects of the DataSource
 * @param userAllowedToWriteIds The list of ids of the users who can write objects of the DataSource. Users allowed to
 * write are automatically added the the users allowed to read if not present
 * @param userAllowedToDeleteIds The list of ids of the users who can delete objects of the DataSource. Users allowed
 * to write are automatically added the the users allowed to read and write if not present
*/
data class DataSource(
    val id: String?,
    val dataProviderId: String,
    val name: String,
    val userAllowedToReadIds: Set<String>,
    val userAllowedToWriteIds: Set<String>,
    val userAllowedToDeleteIds: Set<String>
) : Loggable {

    override fun getLoggingId(): String {
        return "'${this.name} [id: ${this.id}]'"
    }
}