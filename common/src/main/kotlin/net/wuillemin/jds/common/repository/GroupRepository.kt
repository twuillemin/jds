package net.wuillemin.jds.common.repository

import net.wuillemin.jds.common.entity.Group
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

/**
 * The repository of the groups that are stored in the database
 */
@Repository
interface GroupRepository : CrudRepository<Group, String> {

    /**
     * Search a group by its name
     *
     * @param name The group name to search
     * @return a list of group object, that should contain 1 object is the group is found
     */
    fun findByName(name: String): List<Group>
}