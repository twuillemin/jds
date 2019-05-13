package net.wuillemin.jds.common.repository

import net.wuillemin.jds.common.entity.User
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

/**
 * The repository of the user that are stored in the database
 */
@Repository
interface UserRepository : CrudRepository<User, String> {

    /**
     * Search a user by its userName.
     *
     * @param userName The userName to search
     * @return a list of user object, that should contain 1 object is the user is found
     */
    fun findByUserName(userName: String): List<User>
}