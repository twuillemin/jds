package net.wuillemin.jds.common.exception

/**
 * An exception that should be thrown when an authentication (would it be an initial authentication, a refresh or
 * whatever related to authentication) is rejected.
 *
 * @param code The code of the exception
 * @param args The arguments of the exceptions
 */
class AuthenticationRejectedException(
    val code: ExceptionCode,
    vararg val args: Any?
) : RuntimeException() {

    companion object {
        private const val serialVersionUID = 1L
    }
}