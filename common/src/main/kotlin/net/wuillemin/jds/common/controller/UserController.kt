package net.wuillemin.jds.common.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import net.wuillemin.jds.common.entity.User
import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.common.exception.DeniedPermissionException
import net.wuillemin.jds.common.exception.E
import net.wuillemin.jds.common.security.server.AuthenticationToken
import net.wuillemin.jds.common.service.UserService
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.annotations.ApiIgnore

/**
 * Controller for user related operations
 *
 * @param userService The service for getting [User] information
 * @param logger The logger
 */
@RestController
@RequestMapping("/api/common/v1/users")
@Api(tags = ["Users and groups API"], description = "Manage users and groups")
@Secured(value = ["ROLE_USER", "ROLE_ADMIN"])
class UserController(
    private val userService: UserService,
    private val logger: Logger
) {

    /**
     * Return all the users of the application
     *
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Get all the users of the application")
    @GetMapping
    fun getUsers(
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<List<User>> {

        logger.debug("getUsers: ${authentication.getLoggingId()}")

        val users = if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {
            listOf(userService.getUserById(authentication.permission.userId))
        }
        else {
            userService.getUsers()
        }

        return ResponseEntity(users, HttpStatus.OK)
    }

    /**
     * Retrieve the detail of a single user. Apart from the admin, only the user himself is authorized to retrieve
     * the details
     *
     * @param userId The id of the user to retrieve
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Get a single user by its id")
    @GetMapping("{id}")
    fun getUserById(
        @PathVariable("id") userId: Long,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<User> {

        logger.debug("getById: ${authentication.getLoggingId()}")

        val requestedUser = userService.getUserById(userId)

        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (authentication.permission.userId != userId) {
                throw DeniedPermissionException(E.controller.user.getDenied, authentication.permission.userId, requestedUser.getLoggingId())
            }
        }

        return ResponseEntity(requestedUser, HttpStatus.OK)
    }

    /**
     * Update an existing user. Ids of the object and the query must be equals
     *
     * @param userId The userId of the user to update
     * @param user The user to create. If an userId is given in the object, it will be ignored
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Update an existing user")
    @PutMapping("{id}")
    fun updateUser(
        @PathVariable("id") userId: Long,
        @RequestBody user: User,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<User> {

        logger.debug("updateUser: ${authentication.getLoggingId()}")

        if (user.id != userId) {
            throw BadParameterException(E.controller.user.updateDifferentIds)
        }

        val requestedUser = userService.getUserById(userId)

        val updatedUser = if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (authentication.permission.userId != userId) {
                throw DeniedPermissionException(E.controller.user.updateDenied, authentication.permission.userId, requestedUser.getLoggingId())
            }

            userService.updateSafeUser(user)
        }
        else {
            userService.updateUserAllOptions(user)
        }

        return ResponseEntity(updatedUser, HttpStatus.OK)
    }

    /**
     * Delete a user
     *
     * @param userId The id of the user to update
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Delete an existing user")
    @DeleteMapping("{id}")
    fun deleteUser(
        @PathVariable("id") userId: Long,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Void> {

        logger.debug("deleteUser: ${authentication.getLoggingId()}")

        val requestedUser = userService.getUserById(userId)

        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            throw DeniedPermissionException(E.controller.user.deleteDenied, authentication.permission.userId, requestedUser.getLoggingId())
        }

        userService.deleteUser(requestedUser)

        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}