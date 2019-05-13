package net.wuillemin.jds.common.security

/**
 * A simple class holding together the various constant needed to process the authentication
 */
class JWTConstant {
    companion object {

        /**
         * The name of the HTTP header holding the authorization
         */
        const val HTTP_HEADER_AUTHORIZATION: String = "Authorization"

        /**
         * The prefix of the token in the HTTP header
         */
        const val HTTP_HEADER_BEARER_PREFIX: String = "Bearer "

        /**
         * The prefix of the token in the HTTP header
         */
        const val HTTP_HEADER_BASIC_PREFIX: String = "Basic "

        /**
         * The name of the claims having the JDS roles of the user
         */
        const val PERMISSION_CLAIMS_NAME: String = "jds-rights"

        /**
         * The name of the claims having the JDS roles of the user
         */
        const val ROLES_CLAIMS_NAME: String = "jds-role"

    }
}