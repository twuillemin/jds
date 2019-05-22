package net.wuillemin.jds.common.controller

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import net.wuillemin.jds.common.entity.Group
import net.wuillemin.jds.common.entity.User
import net.wuillemin.jds.common.exception.DeniedPermissionException
import net.wuillemin.jds.common.exception.E
import net.wuillemin.jds.common.security.server.AuthenticationToken
import net.wuillemin.jds.common.service.GroupService
import net.wuillemin.jds.common.service.UserService
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.annotations.ApiIgnore

/**
 * Controller for operations on groups
 *
 * @param groupService The service for getting [Group] information
 * @param userService The service for getting [User] information
 * @param logger The logger
 */
@RestController
@RequestMapping("/api/common/v1/groups")
@Secured(value = ["ROLE_USER", "ROLE_ADMIN"])
@Api(tags = ["Users and groups API"], description = "Manage users and groups")
class GroupController(
    private val userService: UserService,
    private val groupService: GroupService,
    private val logger: Logger
) {

    /**
     * Get all groups of the application
     *
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Get all the groups")
    @GetMapping
    fun getGroups(
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<List<Group>> {

        logger.warn("getGroups: ${authentication.getLoggingId()}")

        val groups = if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {
            groupService.findGroupByIds(authentication.permission.userGroupIds)
        }
        else {
            groupService.getGroups()
        }

        return ResponseEntity(groups, HttpStatus.OK)
    }

    /**
     * Get a single group
     *
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Get a single groups")
    @GetMapping("{id}")
    fun getGroupById(
        @PathVariable("id") groupId: Long,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Group> {

        logger.warn("getGroup: ${authentication.getLoggingId()}")

        val group = groupService.getGroupById(groupId)

        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {
            throw DeniedPermissionException(E.controller.group.getDenied, authentication.permission.userId, group.getLoggingId())
        }

        return ResponseEntity(group, HttpStatus.OK)
    }

    /**
     * Create a new group. The current user is set as administrator of the group
     *
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Create a new group", notes = "The user creating the group is automatically defined as its administrator")
    @PostMapping
    fun createGroup(
        @RequestBody group: Group,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Group> {

        logger.warn("createGroup: ${authentication.getLoggingId()}")

        val creator = userService.getUserById(authentication.permission.userId)

        return ResponseEntity(groupService.addGroup(group, creator), HttpStatus.CREATED)
    }

    /**
     * Define the administrators of a group
     *
     * @param groupId The groupId of the group to update
     * @param administratorIds The list of the user ids of the administrators
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Update the administrators of a group")
    @PutMapping("{id}/administrators")
    fun setGroupAdministrators(
        @PathVariable("id") groupId: Long,
        @RequestBody administratorIds: List<Long>,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Group> {

        logger.warn("setGroupAdministrators: ${authentication.getLoggingId()}")

        val group = groupService.getGroupById(groupId)

        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!group.administratorIds.contains(authentication.permission.userId)) {
                throw DeniedPermissionException(E.controller.group.setAdministratorsDenied, authentication.permission.userId, group.getLoggingId())
            }
        }

        val newAdministrators = userService.getUserByIds(administratorIds.toSet())

        return ResponseEntity(groupService.setGroupAdministrators(group, newAdministrators), HttpStatus.OK)
    }

    /**
     * Define the users of a group
     *
     * @param groupId The groupId of the group to update
     * @param userIds The list of the user ids
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Update the users of a group")
    @PutMapping("{id}/users")
    fun setGroupUsers(
        @PathVariable("id") groupId: Long,
        @RequestBody userIds: List<Long>,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Group> {

        logger.warn("setGroupUsers: ${authentication.getLoggingId()}")

        val group = groupService.getGroupById(groupId)

        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {

            if (!group.administratorIds.contains(authentication.permission.userId)) {
                throw DeniedPermissionException(E.controller.group.setUsersDenied, authentication.permission.userId, group.getLoggingId())
            }
        }

        val newUsers = userService.getUserByIds(userIds.toSet())

        return ResponseEntity(groupService.setGroupUsers(group, newUsers), HttpStatus.OK)
    }

    /**
     * Delete a group
     *
     * @param id The id of the group to update
     * @param authentication The authentication of the requester
     */
    @ApiOperation("Delete a group")
    @DeleteMapping("{id}")
    fun deleteGroup(
        @PathVariable("id") id: Long,
        @RequestBody userIds: List<Long>,
        @ApiIgnore authentication: AuthenticationToken
    ): ResponseEntity<Void> {

        logger.warn("deleteGroup: ${authentication.getLoggingId()}")

        val group = groupService.getGroupById(id)

        if (authentication.authorities.none { it.toString() == "ROLE_ADMIN" }) {
            throw DeniedPermissionException(E.controller.group.deleteDenied, authentication.permission.userId, group.getLoggingId())
        }

        groupService.deleteGroup(group)

        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}