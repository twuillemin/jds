package net.wuillemin.jds.common.exception

/**
 * A specific exception that must be thrown when a corruption of the database is detected. As mongo
 * offer no constraint in database, constraints (such as uniqueness) are done by hand.
 *
 * @param code The code of the exception
 * @param args The arguments of the exceptions
 */
class CriticalConstraintException(
    val code: ExceptionCode,
    vararg val args: Any?) : RuntimeException() {

    companion object {
        private const val serialVersionUID = 1L
    }
}