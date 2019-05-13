package net.wuillemin.jds.common.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import net.wuillemin.jds.common.entity.User
import net.wuillemin.jds.common.service.UserService
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for registering new users registration.
 *
 * @param userService The service for getting [User] information
 * @param logger The logger
 */
@RestController
@RequestMapping("/public/v1/registration")
@Api(tags = ["Registration API"], description = "Public facing API - No login required")
class RegistrationController(
    private val userService: UserService,
    private val logger: Logger) {

    /**
     * Create a new user
     *
     * @param user The user to create. If an id is given in the object, an exception will be thrown
     */
    @ApiOperation("Create a new user")
    @PostMapping()
    fun createUser(
        @RequestBody user: User): ResponseEntity<User> {

        logger.info("createUser($user)")

        // Create an user with a safe set of options
        return ResponseEntity(userService.addSafeUser(user), HttpStatus.CREATED)
    }
}