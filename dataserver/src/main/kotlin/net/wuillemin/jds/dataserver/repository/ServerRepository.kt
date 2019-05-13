package net.wuillemin.jds.dataserver.repository

import net.wuillemin.jds.dataserver.entity.model.Server
import org.springframework.data.mongodb.repository.MongoRepository

/**
 * The repository for [Server] objects
 */
interface ServerRepository : MongoRepository<Server, String> {

    /**
     * Return the list of all servers related to a group
     * @param groupIds The id of the groups
     * @return a list of servers
     */
    fun findByGroupIdIn(groupIds: List<String>): List<Server>
}