package net.wuillemin.jds.common.entity

/**
 * Define a group of users
 *
 * @param id The id of the group
 * @param name The name of the group
 * @param administratorIds The ids of the user that are administrator of the group. Members of the administrators are
 * also automatically added to the group users
 * @param userIds The ids of the user that are simple user of the group
 */
data class Group(
    val id: String?,
    val name: String,
    val administratorIds: Set<String>,
    val userIds: Set<String>) : Loggable {

    override fun getLoggingId(): String {
        return "'${this.name} [id: ${this.id}]'"
    }
}