package net.wuillemin.jds.common.exception

/**
 * A specific exception that must be thrown when a query can not be achieved for
 * model constraint reason. The main difference with BadParameterException is that
 * this exception means that the client could not foresee the error.
 *
 * @param code The code of the exception
 * @param args The arguments of the exceptions
 */
class ConstraintException(
    val code: ExceptionCode,
    vararg val args: Any?
) : RuntimeException() {

    companion object {
        private const val serialVersionUID = 1L
    }
}