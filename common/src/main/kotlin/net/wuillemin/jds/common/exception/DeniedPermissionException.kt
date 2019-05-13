package net.wuillemin.jds.common.exception

/**
 * A specific exception that must be thrown when a query can not be achieved for
 * authorization / permissions reasons.
 *
 * @param code The code of the exception
 * @param userId The id of the user implied
 * @param args The arguments of the exceptions
 */
class DeniedPermissionException(
    val code: ExceptionCode,
    val userId: String,
    vararg val args: Any?) : RuntimeException() {

    companion object {
        private const val serialVersionUID = 1L
    }
}