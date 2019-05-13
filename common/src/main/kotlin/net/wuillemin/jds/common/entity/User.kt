package net.wuillemin.jds.common.entity

/**
 * A data class holding an user as saved in the database
 *
 * @param id The id of the user
 * @param userName The userName of the user (aka login)
 * @param password The password of the user, hashed
 * @param firstName The first name of the user
 * @param lastName The second name of the user
 * @param enabled If the use is enabled or disabled
 * @param profile The profiles of the user
 * @param participatingGroupIds The group that the user declares membership to
 */
data class User(
    val id: String?,
    val userName: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val enabled: Boolean,
    val profile: Profile,
    val participatingGroupIds: Set<String>) : Loggable {

    override fun getLoggingId(): String {
        return "'${this.userName} [id: ${this.id}]'"
    }
}
