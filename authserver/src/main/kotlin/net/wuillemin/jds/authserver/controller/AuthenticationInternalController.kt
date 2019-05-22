package net.wuillemin.jds.authserver.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import net.wuillemin.jds.authserver.service.AuthenticationService
import net.wuillemin.jds.common.dto.TokenRequest
import net.wuillemin.jds.common.dto.TokenResponse
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for authenticating user and generating token
 *
 * @param authenticationService The service for authentication
 * @param logger The logger
 */
@RestController
@RequestMapping("/authentication/v1/internal")
@Api(tags = ["Authentication"], description = "Authenticate the users")
class AuthenticationInternalController(
    private val authenticationService: AuthenticationService,
    private val logger: Logger
) {

    /**
     * Login a user in the system
     *
     * @param request The authentication request
     */
    @ApiOperation("Authenticate a user")
    @PostMapping("/login")
    fun loginInternal(@RequestBody request: TokenRequest): ResponseEntity<TokenResponse> {

        logger.debug("login(${request.userName})")

        return ResponseEntity(authenticationService.loginByTokenRequest(request), HttpStatus.OK)
    }
}