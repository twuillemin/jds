package net.wuillemin.jds.common.exception

/**
 * Class representing the code that is passed to the internal exception.
 *
 * @param code The code of the exception
 */
data class ExceptionCode(
    /**
     * The code value that must correspond to a localizable string in the .properties file
     */
    val code: String
)