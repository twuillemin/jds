package net.wuillemin.jds.dataserver.repository

import net.wuillemin.jds.dataserver.entity.model.Schema
import net.wuillemin.jds.dataserver.entity.model.Server
import org.springframework.data.mongodb.repository.MongoRepository

/**
 * The repository for [Schema] objects
 */
interface SchemaRepository : MongoRepository<Schema, String> {

    /**
     * Return the list of schemas referencing the given [Server] id.
     *
     * @param serverId The id of the server referenced
     * @return the list of schema referencing the giver server id
     */
    fun findByServerId(serverId: String): List<Schema>

    /**
     * Return the list of schemas referencing the given [Server] ids.
     *
     * @param serverIds The list of ids of the server referenced
     * @return the list of schema referencing the giver server ids
     */
    fun findByServerIdIn(serverIds: List<String>): List<Schema>
}