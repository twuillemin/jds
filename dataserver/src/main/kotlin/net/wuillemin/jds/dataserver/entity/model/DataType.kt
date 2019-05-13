package net.wuillemin.jds.dataserver.entity.model

/**
 * The type of data that can be processed by the data server
 */
enum class DataType {

    /**
     * String of character
     */
    STRING,
    /**
     * Integer number
     */
    LONG,
    /**
     * Float number
     */
    DOUBLE,
    /**
     * Boolean
     */
    BOOLEAN,
    /**
     * Date
     */
    DATE,
    /**
     * Time with timezone
     */
    TIME,
    /**
     * Date time with timezone
     */
    DATE_TIME,
    /**
     * List of String (used by lookups)
     */
    LIST_OF_STRINGS
}