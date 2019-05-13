package net.wuillemin.jds.common.security

/**
 * The rights of a user. this class is not persisted and is returned in the token
 *
 * @param userId The id of the user
 * @param adminGroupIds The id of the groups that the user administrate
 * @param userGroupIds The id of the groups that the user is just a simple user
 */
data class UserPermission(
    val userId: String,
    val adminGroupIds: List<String>,
    val userGroupIds: List<String>
)
