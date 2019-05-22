package net.wuillemin.jds.common.service

import net.wuillemin.jds.common.entity.Group
import net.wuillemin.jds.common.entity.User
import net.wuillemin.jds.common.event.GroupCreatedEvent
import net.wuillemin.jds.common.event.GroupDeletedEvent
import net.wuillemin.jds.common.event.GroupUpdatedEvent
import net.wuillemin.jds.common.event.UserUpdatedEvent
import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.common.exception.C
import net.wuillemin.jds.common.exception.ConstraintException
import net.wuillemin.jds.common.exception.CriticalConstraintException
import net.wuillemin.jds.common.exception.E
import net.wuillemin.jds.common.exception.NotFoundException
import net.wuillemin.jds.common.repository.GroupRepository
import net.wuillemin.jds.common.repository.UserRepository
import org.slf4j.Logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service


/**
 * Base service for CRUD operation on a [Group] object.
 *
 * @param userRepository The repository of [User]
 * @param groupRepository The repository of [Group]
 * @param applicationEventPublisher The message queue for publishing events in the application
 * @param logger The logger
 */
@Service
class GroupService(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val logger: Logger
) {

    /**
     * Get the list of all groups.
     *
     * @return the list of all groups
     */
    fun getGroups(): List<Group> {
        return groupRepository.findAll().toList()
    }

    /**
     * Try to load the groups with the given ids. If some ids does not exist, no exception is thrown
     *
     * @param ids The id of the groups to find and load
     * @return the groups
     */
    fun findGroupByIds(ids: Iterable<Long>): List<Group> {
        return groupRepository.findAllById(ids).toList()
    }

    /**
     * Try to load the groups for the given user id. If the user id does not exist, no exception is thrown
     *
     * @param userId The id of the user for which to load the groups
     * @return the groups
     */
    fun findGroupByUserId(userId: Long): List<Group> {
        return groupRepository.findAllByUserId(userId).toList()
    }

    /**
     * Get a group by its id. If the group is not found a NotFoundException exception is thrown.
     *
     * @param id The id of the group to retrieve
     * @return the group
     */
    fun getGroupById(id: Long): Group {
        return groupRepository.findById(id).orElseThrow {
            NotFoundException(C.notFound.idClass, id, Group::class)
        }
    }

    /**
     * Get a group by its name. If the group is not found a NotFoundException thrown. If multiple groups
     * exist with the same name a ConstraintException is thrown
     *
     * @param name The name to retrieve
     * @return the group
     */
    fun getGroupByName(name: String): Group {
        // Get the group with the wanted groupName
        val groups = groupRepository.findAllByName(name)
        return when (groups.size) {
            0    -> {
                throw NotFoundException(C.notFound.valueAttributeClass, name, "name", Group::class)
            }
            1    -> groups[0]
            else -> {
                throw CriticalConstraintException(E.service.group.getMultipleGroupsSameName, name)
            }
        }
    }

    /**
     * Create a new group in the database. If an id is given, an exception is thrown. The user must be persisted before
     * the group as it is updated.
     *
     * @param group The group to create
     * @param user The user that will be defined as the first administrator of the group
     * @return the group created
     */
    fun addGroup(group: Group, user: User): Group {

        return group.id
            ?.let {
                throw BadParameterException(E.service.group.createAlreadyPersisted)
            }
            ?: run {
                user.id
                    ?.let { userId ->

                        if (groupRepository.findAllByName(group.name).isNotEmpty()) {
                            throw ConstraintException(E.service.group.createNameAlreadyExists)
                        }

                        // Create the group adding the creator to the administrators and the users
                        val groupToCreate = group.copy(
                            administratorIds = group.administratorIds + userId,
                            userIds = group.userIds + group.administratorIds + userId)

                        groupRepository
                            .save(groupToCreate)
                            .also { groupAdded ->
                                // Publish the information
                                applicationEventPublisher.publishEvent(GroupCreatedEvent(groupAdded.id!!))
                                logger.info("addGroup: The group ${groupAdded.getLoggingId()} was created")
                            }
                    }
                    ?: throw BadParameterException(E.service.group.createCreatorNotPersisted)
            }
    }

    /**
     * Define the users of a group. This function is sending an event for the updated group and the updated users.
     *
     * @param group The group to update
     * @param users The users that will be defined as the users of the group
     * @return the group updated
     */
    fun setGroupUsers(group: Group, users: List<User>): Group {

        return group.id
            ?.let { groupId ->

                // Ensure a clean list of user ids
                val newUserIds = users
                    .map { (id) ->
                        id
                            ?.let { it }
                            ?: throw BadParameterException(E.service.group.setUsersWithUsersNotPersisted)
                    }
                    .toSet() + group.administratorIds

                // Update the group
                groupRepository
                    .save(group.copy(userIds = newUserIds))
                    .also {
                        // Publish the information
                        applicationEventPublisher.publishEvent(GroupUpdatedEvent(groupId))
                    }
            }
            ?: throw BadParameterException(E.service.group.setUsersWithGroupNotPersisted)
    }

    /**
     * Define the administrators of a group. This function is sending an event for the updated group and the updated users.
     *
     * @param group The group to update
     * @param administrators The users that will be defined as the administrators of the group
     * @return the group updated
     */
    fun setGroupAdministrators(group: Group, administrators: List<User>): Group {

        return group.id
            ?.let { groupId ->
                // Ensure a clean list of admin ids
                val newAdministratorIds = administrators
                    .map { (id) ->
                        id
                            ?.let { it }
                            ?: throw BadParameterException(E.service.group.setAdministratorsWithUsersNotPersisted)

                    }
                    .toSet()

                // Ensure that there is at least one active admin left
                if (administrators.count { user -> user.enabled } < 1) {
                    throw ConstraintException(E.service.group.setAdministratorsNoAdministratorLeft)
                }

                val newUserIds = group.userIds + newAdministratorIds

                groupRepository
                    // Update the group
                    .save(group.copy(
                        administratorIds = newAdministratorIds,
                        userIds = newUserIds))
                    .also {
                        // Send the message for updating
                        applicationEventPublisher.publishEvent(GroupUpdatedEvent(groupId))
                    }
            }
            ?: throw BadParameterException(E.service.group.setAdministratorsWithGroupNotPersisted)
    }

    /**
     * Remove a user from a group, would it be in the administrators or the users of a group. This function is sending
     * an event for the updated group and the updated users.
     *
     * @param group The group from which to remove the user
     * @param user The user to be removed
     */
    fun removeUserFromGroup(group: Group, user: User): Group {

        return group.id
            ?.let { groupId ->
                user.id
                    ?.let { userId ->

                        val newAdministratorIds = group.administratorIds.filter { id -> id != userId }.toSet()
                        val newUserIds = group.userIds.filter { id -> id != userId }.toSet()

                        // Ensure that there is at least one active admin left
                        val newAdministrator = userRepository.findAllById(newAdministratorIds)
                        if (newAdministrator.count { user -> user.enabled } < 1) {
                            throw ConstraintException(E.service.group.removeUserNoAdministratorLeft)
                        }

                        // Clean the group, save it and update the application rights
                        groupRepository
                            .save(group.copy(
                                administratorIds = newAdministratorIds,
                                userIds = newUserIds))
                            .also {
                                applicationEventPublisher.publishEvent(GroupUpdatedEvent(groupId))
                            }
                    }
                    ?: throw BadParameterException(E.service.group.removeUserWithUserNotPersisted)
            }
            ?: throw BadParameterException(E.service.group.removeUserWithGroupNotPersisted)
    }

    /**
     * Delete a group by its id. If the group is not found a NotFoundException exception is thrown.
     *
     * @param group The group to delete
     * @return the group created
     */
    fun deleteGroup(group: Group) {

        group.id
            ?.let { groupId ->
                // Ensure the group is present in the database
                groupRepository.findById(groupId).orElseThrow {
                    NotFoundException(C.notFound.idClass, groupId, Group::class)
                }

                // Delete the group
                groupRepository.delete(group)

                // Send the message
                (group.administratorIds + group.userIds).forEach { userId ->
                    applicationEventPublisher.publishEvent(UserUpdatedEvent(userId))
                }

                applicationEventPublisher.publishEvent(GroupDeletedEvent(groupId))

                logger.info("deleteGroup: The group ${group.getLoggingId()} was deleted")
            }
            ?: throw BadParameterException(E.service.group.deleteGroupNotPersisted)
    }
}