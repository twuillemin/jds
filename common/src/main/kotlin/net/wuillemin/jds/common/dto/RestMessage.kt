package net.wuillemin.jds.common.dto

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * A DTO class used for returning an exception / error message
 *
 * @param message A single message
 * @param messages A list of messages
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class RestMessage(
    val message: String? = null,
    val messages: List<String>? = null
)