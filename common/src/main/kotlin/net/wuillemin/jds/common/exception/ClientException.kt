package net.wuillemin.jds.common.exception

/**
 * An exception dedicated at keeping the error raised when the backend act as a client, for example
 * requesting data from an external webservice.
 *
 * @param code The code of the exception
 * @param args The arguments of the exceptions
 */
class ClientException(
    val code: ExceptionCode,
    vararg val args: Any?) : RuntimeException() {

    companion object {
        private const val serialVersionUID = 1L
    }
}