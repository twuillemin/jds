package net.wuillemin.jds.common.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * The request to be made for requesting a new authentication token
 *
 * @param userName The name of the user
 * @param password The password of the user (in clear)
 */
data class TokenRequest(
    @JsonProperty val userName: String,
    @JsonProperty val password: String
)
