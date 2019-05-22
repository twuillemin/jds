package net.wuillemin.jds.common.entity

/**
 * The various profile of the application
 */
enum class Profile constructor(
    /**
     * The name of the role as used by Spring
     */
    val springRoleName: String
) {

    /**
     * Admin user
     */
    ADMIN("ROLE_ADMIN"),

    /**
     * Service user
     */
    SERVICE("ROLE_SERVICE"),

    /**
     * Standard user
     */
    USER("ROLE_USER")
}
