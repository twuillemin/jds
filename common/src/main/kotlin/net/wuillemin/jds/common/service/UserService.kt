package net.wuillemin.jds.common.service

import net.wuillemin.jds.common.entity.Group
import net.wuillemin.jds.common.entity.Profile
import net.wuillemin.jds.common.entity.User
import net.wuillemin.jds.common.event.GroupUpdatedEvent
import net.wuillemin.jds.common.event.UserCreatedEvent
import net.wuillemin.jds.common.event.UserDeletedEvent
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
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service


/**
 * Base service for CRUD operation on an [User] object.
 *
 * @param userRepository The repository of [User]
 * @param groupRepository The repository of [Group]
 * @param passwordEncoder The encoder of Passwords
 * @param applicationEventPublisher The message queue for publishing events in the application
 * @param logger The logger
 */
@Service
class UserService(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val passwordEncoder: PasswordEncoder,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val logger: Logger) {

    /**
     * Get the list of all users.
     *
     * @return the list of all users
     */
    fun getUsers(): List<User> {
        return userRepository.findAll().toList()
    }

    /**
     * Get a user by its id. If the user is not found a NotFoundException exception is thrown.
     *
     * @param id The id of the user
     * @return the user
     */
    fun getUserById(id: String): User {
        return userRepository.findById(id).orElseThrow {
            NotFoundException(C.notFound.idClass, id, User::class)
        }
    }

    /**
     * Get a list of users by their ids. The returned list will contains all the requested users or an exception will be thrown
     * The order is not guaranteed
     *
     * @param ids a set of user ids
     * @return the users
     */
    fun getUserByIds(ids: Set<String>): List<User> {

        val users = userRepository.findAllById(ids).toList()

        if (ids.size != users.size) {
            val allLoadedUserIds = users.mapNotNull { it.id }
            val notFoundIds = ids.filter { id -> !allLoadedUserIds.contains(id) }
            throw NotFoundException(C.notFound.valueAttributeClass, notFoundIds, "id", User::class)
        }

        return users
    }

    /**
     * Get a user by name. If the user is not found a NotFoundException exception is thrown.
     * @param userName the name of the user
     * @return the user
     */
    fun getUserByUserName(userName: String): User {
        // Get the user with the wanted userName
        val users = userRepository.findByUserName(userName)
        return when (users.size) {
            0    -> throw throw NotFoundException(C.notFound.valueAttributeClass, userName, "name", User::class)
            1    -> users[0]
            else -> {
                throw CriticalConstraintException(E.service.user.getMultipleUsersSameName, userName)
            }
        }
    }

    /**
     * Create a new user in the database. If an id is given, an exception is thrown. The password must be sent in clear so
     * that the service will hash it. This function is only allowing safe parameters to be defined and so should be used
     * for general use
     *
     * @param user The user to create
     * @return the user created
     */
    fun addSafeUser(user: User): User {

        return addUser(
            user.copy(
                enabled = true,
                profile = Profile.USER))
    }

    /**
     * Internal function to create an user
     *
     * @param user The user to create
     * @return the user created
     */
    private fun addUser(user: User): User {

        return user.id
            ?.let {
                throw BadParameterException(E.service.user.createAlreadyPersisted)
            }
            ?: run {

                if (userRepository.findByUserName(user.userName).isNotEmpty()) {
                    throw ConstraintException(E.service.user.createNameAlreadyExists)
                }

                if (user.participatingGroupIds.isNotEmpty()) {
                    throw BadParameterException(E.service.user.createUserWithGroupsNotEmpty)
                }

                userRepository
                    .save(
                        user.copy(password = passwordEncoder.encode(user.password)))
                    .also { user ->
                        applicationEventPublisher.publishEvent(UserCreatedEvent(user.id!!))
                        logger.info("addUser: The user ${user.getLoggingId()} was created")
                    }
            }
    }

    /**
     * Update a user. The password must be sent in clear so that the service will hash it.
     * This function allows to set every parameters and so should only be used by administrators.
     *
     * @param user the new information for the user
     * @return the updated user
     */
    fun updateUserAllOptions(user: User): User {

        return user.id
            ?.let { userId ->
                val existingUser = getUserById(userId)
                updateUser(user, existingUser)
            }
            ?: throw BadParameterException(E.service.user.updateNotPersisted)
    }

    /**
     * Update a user. The password must be sent in clear so that the service will hash it.
     * This function allows to set every parameters and so should only be used by administrators.
     *
     * @param user the new information for the user
     * @return the updated user
     */
    fun updateSafeUser(user: User): User {

        return user.id
            ?.let { userId ->
                val existingUser = getUserById(userId)
                updateUser(
                    user.copy(
                        enabled = existingUser.enabled,
                        profile = Profile.USER),
                    existingUser)
            }
            ?: throw BadParameterException(E.service.user.updateNotPersisted)
    }

    /**
     * Internal function to update an user
     *
     * @param user The user to update
     * @return the user updated
     */
    private fun updateUser(user: User, existingUser: User): User {

        if (user.participatingGroupIds != existingUser.participatingGroupIds) {
            throw BadParameterException(E.service.user.updateWithModifiedGroups)
        }

        if (user.userName != existingUser.userName) {
            if (userRepository.findByUserName(user.userName).isNotEmpty()) {
                throw ConstraintException(E.service.user.updateNameAlreadyExists)
            }
        }

        return userRepository
            .save(
                user.copy(password = passwordEncoder.encode(user.password)))
            .also {
                applicationEventPublisher.publishEvent(UserUpdatedEvent(it.id!!))
            }
    }

    /**
     * Delete a user by its id. If the user is not found a NotFoundException exception is thrown.
     *
     * @param user The user to delete
     * @return the user created
     */
    fun deleteUser(user: User) {

        user.id
            ?.let { userId ->

                val groups = groupRepository.findAllById(user.participatingGroupIds)

                val orphanGroups = groups.filter { (it.administratorIds - userId).isEmpty() }
                if (orphanGroups.isNotEmpty()) {
                    throw ConstraintException(E.service.user.deleteNoAdministratorLeft, user.getLoggingId(), orphanGroups)
                }

                // Remove the user from the groups
                groups.forEach { group ->
                    groupRepository
                        .save(
                            group.copy(
                                administratorIds = group.administratorIds - userId,
                                userIds = group.userIds - userId))
                }

                // Delete the user
                userRepository.delete(user)

                // Inform the world
                user.participatingGroupIds.forEach { groupId ->
                    applicationEventPublisher.publishEvent(GroupUpdatedEvent(groupId))
                }
                applicationEventPublisher.publishEvent(UserDeletedEvent(userId))

                logger.info("deleteUser: The user ${user.getLoggingId()} was deleted")

            }
            ?: throw BadParameterException(E.service.user.deleteUserNotPersisted)
    }
}