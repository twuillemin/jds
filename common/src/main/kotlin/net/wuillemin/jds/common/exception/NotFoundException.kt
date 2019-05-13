package net.wuillemin.jds.common.exception

/**
 * Exception to be thrown when an expected object can not be found.
 *
 * @param code The code of the exception
 * @param args The arguments of the exceptions
 */
class NotFoundException(
    val code: ExceptionCode,
    vararg val args: Any?) : RuntimeException() {

    companion object {
        private const val serialVersionUID = 1L
    }
}