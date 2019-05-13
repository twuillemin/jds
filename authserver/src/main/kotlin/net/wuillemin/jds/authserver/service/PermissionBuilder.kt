package net.wuillemin.jds.authserver.service

import net.wuillemin.jds.authserver.exception.E
import net.wuillemin.jds.common.entity.Group
import net.wuillemin.jds.common.entity.User
import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.common.exception.CriticalConstraintException
import net.wuillemin.jds.common.security.UserPermission
import net.wuillemin.jds.common.service.GroupService
import org.springframework.stereotype.Service

/**
 * Service for building the [UserPermission]
 *
 * @param groupService The service for managing [Group]
 */
@Service
class PermissionBuilder(private val groupService: GroupService) {


    /**
     * Find the rights of the [User]
     *
     * @param user The user
     * @return a pair of lists. The first list is the list of ids of the group that the user is administrator. The
     * second list is the list of the application ids that the user can access (without being administrator of the
     * group)
     */
    fun buildPermission(user: User): UserPermission {

        return user.id
            ?.let { userId ->
                // Retrieve all the groups that the user declares itself as member (ensure no duplicate)
                val groups = groupService.findGroupByIds(user.participatingGroupIds).toSet()
                // Spit according to if the user is admin of the group or not
                val partitionAdminOther = groups.partition { it.administratorIds.contains(userId) }
                // Keep the groups for which the user is admin
                val adminGroupIds = partitionAdminOther.first.mapNotNull { it.id }
                // Split according to if the user is user of the group or not
                val partitionUserOther = partitionAdminOther.second.partition { it.userIds.contains(userId) }
                // Keep the groups for which the user is just a user
                val userGroupIds = partitionUserOther.first.mapNotNull { it.id }

                // Try to cheat...
                if (partitionUserOther.second.isNotEmpty()) {
                    throw CriticalConstraintException(E.service.permissionBuilder.incoherentPermission, user, partitionUserOther.second)
                }

                UserPermission(
                    userId,
                    adminGroupIds,
                    userGroupIds)
            }
            ?: throw BadParameterException(E.service.permissionBuilder.userNotPersisted)
    }
}