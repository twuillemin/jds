package net.wuillemin.jds.authserver.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import net.wuillemin.jds.authserver.service.AuthenticationService
import net.wuillemin.jds.common.dto.RefreshRequest
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
@RequestMapping("/authentication/v1")
@Api(tags = ["Authentication"], description = "Authenticate the users")
class AuthenticationCommonController(
    private val authenticationService: AuthenticationService,
    private val logger: Logger) {

    /**
     * Refresh a user authentication in the system
     *
     * @param request The request having the refresh token
     */
    @ApiOperation("Refresh the client token")
    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshRequest): ResponseEntity<TokenResponse> {

        logger.debug("refresh(${request.refreshToken})")

        return ResponseEntity(authenticationService.refresh(request), HttpStatus.OK)
    }

    /**
     * Terminate a user authentication in the system. Note that this method can not disable existing
     * authentication token. Only the refresh token is deactivated
     *
     * @param request The request having the refresh token
     */
    @ApiOperation("Terminate the refresh token")
    @PostMapping("/logout")
    fun logout(@RequestBody request: RefreshRequest): ResponseEntity<Void> {

        logger.debug("refresh(${request.refreshToken})")

        authenticationService.logout(request)

        return ResponseEntity(HttpStatus.OK)
    }
}