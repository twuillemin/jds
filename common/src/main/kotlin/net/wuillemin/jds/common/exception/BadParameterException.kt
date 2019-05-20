package net.wuillemin.jds.common.exception

/**
 * An exception that should be thrown when a function receive an unexpected parameter, for
 * example trying to update an object and giving as a parameter an object with a null id. If the
 * logic flow would be correct (or internally or externally), this should not happen. That's why
 * this exception is present. Generally speaking, this exception denotes errors that could have
 * easily been foreseen by the caller (opposed of ConstraintException).
 *
 * @param code The code of the exception
 * @param args The arguments of the exceptions
 */
class BadParameterException(
    val code: ExceptionCode,
    vararg val args: Any?
) : RuntimeException() {

    companion object {
        private const val serialVersionUID = 1L
    }
}