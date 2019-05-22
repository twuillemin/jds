package net.wuillemin.jds.authserver.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import net.wuillemin.jds.authserver.exception.E
import net.wuillemin.jds.authserver.service.AuthenticationService
import net.wuillemin.jds.common.dto.TokenResponse
import net.wuillemin.jds.common.exception.ClientException
import net.wuillemin.jds.common.service.typeRef
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate

/**
 * Controller for authenticating user and generating token
 *
 * @param authenticationService The service for authentication
 * @param authorizedClientService The Spring OAuth2 support
 */
@RestController
@RequestMapping("/authentication/v1/external")
@Api(tags = ["Authentication"], description = "Authenticate the users")
class AuthenticationExternalController(
    private val authorizedClientService: OAuth2AuthorizedClientService,
    private val authenticationService: AuthenticationService
) {

    /**
     * Login a user coming with google authentication
     */
    @ApiOperation("Authenticate a user")
    @GetMapping("/login")
    fun loginExternal(): ResponseEntity<TokenResponse> {

        return getGoogleInformation()["email"]
            ?.let { email ->
                ResponseEntity(authenticationService.loginFromExternalSource(email.toString()), HttpStatus.OK)
            }
            ?: throw ClientException(E.controller.external.noEmail)
    }

    /**
     * Get Google information from a user (if available)
     */
    @ApiOperation("Get Google information from a user")
    @GetMapping("/information")
    fun information(): ResponseEntity<Map<Any, Any>> {

        return ResponseEntity(getGoogleInformation(), HttpStatus.OK)
    }

    /**
     * Get all the information given by google callback. Information are retrieved from the context
     *
     * @return The information as map
     */
    private fun getGoogleInformation(): Map<Any, Any> {

        val authentication = SecurityContextHolder
            .getContext()
            .authentication

        val oauthToken = authentication as OAuth2AuthenticationToken

        val client = authorizedClientService.loadAuthorizedClient<OAuth2AuthorizedClient>(
            oauthToken.authorizedClientRegistrationId,
            oauthToken.name
        )

        val accessToken = client.accessToken.tokenValue

        return try {
            client?.clientRegistration?.providerDetails?.userInfoEndpoint?.uri
                ?.let { uri ->
                    val restTemplate = RestTemplate()
                    val headers = HttpHeaders()
                    headers.add(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                    val entity = HttpEntity("", headers)
                    restTemplate.exchange(uri, HttpMethod.GET, entity, typeRef<Map<Any, Any>>()).body
                }
                ?: throw ClientException(E.controller.external.googleNoResponse)
        }
        catch (e: Exception) {
            throw ClientException(E.controller.external.googleOtherError, e.message)
        }
    }
}