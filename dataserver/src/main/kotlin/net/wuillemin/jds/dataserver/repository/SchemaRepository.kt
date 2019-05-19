package net.wuillemin.jds.dataserver.repository

import net.wuillemin.jds.dataserver.entity.model.Schema
import net.wuillemin.jds.dataserver.entity.model.Server
import org.springframework.data.repository.CrudRepository

/**
 * The repository for [Schema] objects
 */
interface SchemaRepository : CrudRepository<Schema, Long> {

    /**
     * Return the list of schemas referencing the given [Server] id.
     *
     * @param serverId The id of the server referenced
     * @return the list of schema referencing the giver server id
     */
    fun findByServerId(serverId: Long): List<Schema>

    /**
     * Return the list of schemas referencing the given [Server] ids.
     *
     * @param serverIds The list of ids of the server referenced
     * @return the list of schema referencing the giver server ids
     */
    fun findByServerIdIn(serverIds: List<Long>): List<Schema>
}