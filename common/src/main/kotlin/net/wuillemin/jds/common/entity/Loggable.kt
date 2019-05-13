package net.wuillemin.jds.common.entity

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Interface for object that offer simple logging
 */
interface Loggable {

    /**
     * The name of the object it should be logged
     */
    @JsonIgnore
    fun getLoggingId(): String
}