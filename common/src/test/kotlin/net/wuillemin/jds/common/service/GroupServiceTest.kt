package net.wuillemin.jds.common.service

import net.wuillemin.jds.common.config.CommonConfigDataBaseTest
import net.wuillemin.jds.common.entity.Group
import net.wuillemin.jds.common.entity.Profile
import net.wuillemin.jds.common.entity.User
import net.wuillemin.jds.common.event.GroupCreatedEvent
import net.wuillemin.jds.common.event.GroupDeletedEvent
import net.wuillemin.jds.common.event.GroupUpdatedEvent
import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.common.exception.ConstraintException
import net.wuillemin.jds.common.exception.CriticalConstraintException
import net.wuillemin.jds.common.exception.NotFoundException
import net.wuillemin.jds.common.repository.GroupRepository
import net.wuillemin.jds.common.repository.UserRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEvent
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ApplicationEventMulticaster
import org.springframework.context.support.AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME
import org.springframework.test.context.junit.jupiter.SpringExtension

// Definition of constants
private const val GROUP_ID = 1L
private const val GROUP_ID_1 = 2L
private const val GROUP_ID_2 = 3L
private const val USER_ID = 10L

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [
    CommonConfigDataBaseTest::class,
    GroupService::class])
class GroupServiceTest {

    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var groupRepository: GroupRepository
    @Autowired
    private lateinit var groupService: GroupService
    @Autowired
    private lateinit var applicationContext: ConfigurableApplicationContext

    @Test
    fun `Get group by name should work only if a single object is present`() {

        // Check found if one group
        groupRepository.deleteAll()

        groupRepository.save(Group(GROUP_ID, "azerty", emptySet(), emptySet()))
        val result1 = groupService.getGroupByName("azerty")
        Assertions.assertNotNull(result1)

        // Check exception if not found
        groupRepository.deleteAll()
        Assertions.assertThrows(NotFoundException::class.java) { groupService.getGroupByName("azerty") }

        // Check constraint error if multiple
        groupRepository.deleteAll()
        groupRepository.save(Group(GROUP_ID_1, "azerty", emptySet(), emptySet()))
        groupRepository.save(Group(GROUP_ID_2, "azerty", emptySet(), emptySet()))
        Assertions.assertThrows(CriticalConstraintException::class.java) { groupService.getGroupByName("azerty") }
    }

    @Test
    fun `Create group should save a new group`() {

        groupRepository.deleteAll()
        userRepository.deleteAll()

        val user = userRepository.save(User(null, "userName", "password", "firstName", "lastName", true, Profile.USER))
        val userId = user.id!!

        val groupToCreate = Group(null, "groupName", emptySet(), emptySet())

        // Values sent by the message
        var propagatedGroupId: Long? = null

        // Receive the events
        val applicationEventMulticaster = applicationContext.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME) as ApplicationEventMulticaster
        val listener = fun(event: ApplicationEvent) {
            when (event) {
                is GroupCreatedEvent -> propagatedGroupId = event.groupId
            }
        }
        applicationEventMulticaster.addApplicationListener(listener)

        // Save the group
        val savedGroup = groupService.addGroup(groupToCreate, user)

        // Clean receiving the events
        applicationEventMulticaster.removeApplicationListener(listener)

        // Check group was saved
        val group = groupRepository.findById(savedGroup.id!!).get()
        Assertions.assertNotNull(group)
        Assertions.assertEquals("groupName", group.name)
        Assertions.assertTrue(group.administratorIds.contains(userId))
        Assertions.assertTrue(group.userIds.contains(userId))

        // Check that user and group are related
        val groups = groupRepository.findAllByUserId(userId)
        Assertions.assertNotNull(groups)
        Assertions.assertEquals(1, groups.size)
        Assertions.assertEquals(savedGroup.id, groups[0].id)

        // Check that a message was sent
        Assertions.assertEquals(savedGroup.id!!, propagatedGroupId)
    }

    @Test
    fun `Create group should not save a new group if bad params`() {

        groupRepository.deleteAll()
        userRepository.deleteAll()

        // Group is already persisted
        val groupToCreate1 = Group(GROUP_ID, "groupName", emptySet(), emptySet())
        val userCreating1 = User(USER_ID, "userName", "password", "firstName", "lastName", true, Profile.USER)
        Assertions.assertThrows(BadParameterException::class.java) { groupService.addGroup(groupToCreate1, userCreating1) }

        // User is not persisted
        val groupToCreate2 = Group(GROUP_ID, "groupName", emptySet(), emptySet())
        val userCreating2 = User(USER_ID, "userName", "password", "firstName", "lastName", true, Profile.USER)
        Assertions.assertThrows(BadParameterException::class.java) { groupService.addGroup(groupToCreate2, userCreating2) }

        // Group name already exist
        val groupToCreate3 = Group(null, "groupName", emptySet(), emptySet())
        val userCreating3 = User(USER_ID, "userName", "password", "firstName", "lastName", true, Profile.USER)
        groupRepository.save(Group(GROUP_ID, "groupName", emptySet(), emptySet()))
        Assertions.assertThrows(ConstraintException::class.java) { groupService.addGroup(groupToCreate3, userCreating3) }
    }

    @Test
    fun `Set group users should update users`() {

        groupRepository.deleteAll()
        userRepository.deleteAll()

        // Create some users
        val userToBeAddedToGroup = userRepository.save(User(null, "userName", "password", "firstName", "lastName", true, Profile.USER))
        val userToBeAddedToGroupId = userToBeAddedToGroup.id!!
        val userToStayInGroup = userRepository.save(User(null, "userName", "password", "firstName", "lastName", true, Profile.USER))
        val userToStayInGroupId = userToStayInGroup.id!!
        // This user will removed from group
        val userToBeRemovedFromGroupId = userRepository.save(User(null, "userName", "password", "firstName", "lastName", true, Profile.USER)).id!!
        // This user will stay in group but just as admin
        val adminToStayInGroupId = userRepository.save(User(null, "userName", "password", "firstName", "lastName", true, Profile.USER)).id!!

        // Create the group
        val group = groupRepository.save(Group(null, "groupName", setOf(adminToStayInGroupId), setOf(userToBeRemovedFromGroupId, userToStayInGroupId, adminToStayInGroupId)))
        val groupId = group.id!!

        // Values sent by the message
        var propagatedGroupId: Long? = null

        // Receive the events
        val applicationEventMulticaster = applicationContext.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME) as ApplicationEventMulticaster
        val listener = fun(event: ApplicationEvent) {
            when (event) {
                is GroupUpdatedEvent -> propagatedGroupId = event.groupId
            }
        }
        applicationEventMulticaster.addApplicationListener(listener)

        // Update the users of the group
        groupService.setGroupUsers(group, listOf(userToBeAddedToGroup, userToStayInGroup))

        applicationEventMulticaster.removeApplicationListener(listener)

        // Check group was saved
        val groupUpdated = groupRepository.findById(groupId).get()
        Assertions.assertNotNull(groupUpdated)
        Assertions.assertEquals(1, groupUpdated.administratorIds.size)
        Assertions.assertTrue(groupUpdated.userIds.contains(adminToStayInGroupId))
        Assertions.assertEquals(3, groupUpdated.userIds.size)
        Assertions.assertTrue(groupUpdated.userIds.contains(userToBeAddedToGroupId))
        Assertions.assertTrue(groupUpdated.userIds.contains(userToStayInGroupId))
        Assertions.assertTrue(groupUpdated.userIds.contains(adminToStayInGroupId))

        // Check users to be added was updated
        val groups1 = groupRepository.findAllByUserId(userToBeAddedToGroupId)
        Assertions.assertNotNull(groups1)
        Assertions.assertEquals(1, groups1.size)
        Assertions.assertEquals(groupId, groups1[0].id)

        // Check users to be removed was updated
        val groups2 = groupRepository.findAllByUserId(userToBeRemovedFromGroupId)
        Assertions.assertNotNull(groups2)
        Assertions.assertEquals(0, groups2.size)

        // Check users to stay is still here
        val groups3 = groupRepository.findAllByUserId(userToStayInGroupId)
        Assertions.assertNotNull(groups3)
        Assertions.assertEquals(1, groups3.size)
        Assertions.assertEquals(groupId, groups3[0].id)

        // Check admin was not removed
        val groups4 = groupRepository.findAllByUserId(adminToStayInGroupId)
        Assertions.assertNotNull(groups4)
        Assertions.assertEquals(1, groups4.size)
        Assertions.assertEquals(groupId, groups4[0].id)

        // Check that a message was sent
        Assertions.assertEquals(groupId, propagatedGroupId)
    }

    @Test
    fun `Set group administrators should update administrators`() {

        groupRepository.deleteAll()
        userRepository.deleteAll()

        // Create some users
        val adminToBeAddedToGroup = userRepository.save(User(null, "userName", "password", "firstName", "lastName", true, Profile.USER))
        val adminToBeAddedToGroupId = adminToBeAddedToGroup.id!!
        val adminToStayInGroup = userRepository.save(User(null, "userName", "password", "firstName", "lastName", true, Profile.USER))
        val adminToStayInGroupId = adminToStayInGroup.id!!
        val adminToBeRemovedFromGroup = userRepository.save(User(null, "userName", "password", "firstName", "lastName", true, Profile.USER))
        val adminToBeRemovedFromGroupId = adminToBeRemovedFromGroup.id!!
        val userToStayInGroup = userRepository.save(User(null, "userName", "password", "firstName", "lastName", true, Profile.USER))
        val userToStayInGroupId = userToStayInGroup.id!!


        // Prepare a group and some users
        val group = groupRepository.save(Group(null, "groupName", setOf(adminToStayInGroupId, adminToBeRemovedFromGroupId), setOf(adminToStayInGroupId, adminToBeRemovedFromGroupId, userToStayInGroupId)))
        val groupId = group.id!!

        // Values sent by the message
        var propagatedGroupId: Long? = null

        // Receive the events
        val applicationEventMulticaster = applicationContext.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME) as ApplicationEventMulticaster
        val listener = fun(event: ApplicationEvent) {
            when (event) {
                is GroupUpdatedEvent -> propagatedGroupId = event.groupId
            }
        }
        applicationEventMulticaster.addApplicationListener(listener)

        // Update the users of the group
        groupService.setGroupAdministrators(group, listOf(adminToBeAddedToGroup, adminToStayInGroup))

        applicationEventMulticaster.removeApplicationListener(listener)

        // Check group was saved
        val groupUpdated = groupRepository.findById(groupId).get()
        Assertions.assertNotNull(groupUpdated)
        Assertions.assertEquals(2, groupUpdated.administratorIds.size)
        Assertions.assertTrue(groupUpdated.administratorIds.contains(adminToBeAddedToGroupId))
        Assertions.assertTrue(groupUpdated.administratorIds.contains(adminToStayInGroupId))
        Assertions.assertEquals(4, groupUpdated.userIds.size)
        Assertions.assertTrue(groupUpdated.userIds.contains(adminToBeRemovedFromGroupId))
        Assertions.assertTrue(groupUpdated.userIds.contains(adminToBeAddedToGroupId))
        Assertions.assertTrue(groupUpdated.userIds.contains(adminToStayInGroupId))
        Assertions.assertTrue(groupUpdated.userIds.contains(userToStayInGroupId))

        // Check admin to be added was updated
        val groups1 = groupRepository.findAllByUserId(adminToBeAddedToGroupId)
        Assertions.assertNotNull(groups1)
        Assertions.assertEquals(1, groups1.size)
        Assertions.assertEquals(groupId, groups1[0].id)

        // Check admin to be removed is still present
        val groups2 = groupRepository.findAllByUserId(adminToBeRemovedFromGroupId)
        Assertions.assertNotNull(groups2)
        Assertions.assertEquals(1, groups2.size)
        Assertions.assertEquals(groupId, groups1[0].id)

        // Check admin to stay is still here
        val groups3 = groupRepository.findAllByUserId(adminToStayInGroupId)
        Assertions.assertNotNull(groups3)
        Assertions.assertEquals(1, groups3.size)
        Assertions.assertEquals(groupId, groups3[0].id)

        // Check admin was not removed
        val groups4 = groupRepository.findAllByUserId(userToStayInGroupId)
        Assertions.assertNotNull(groups4)
        Assertions.assertEquals(1, groups4.size)
        Assertions.assertEquals(groupId, groups4[0].id)

        // Check that a message was sent
        Assertions.assertEquals(groupId, propagatedGroupId)
    }

    @Test
    fun `Remove user from group should remove the user but not the last one`() {

        groupRepository.deleteAll()
        userRepository.deleteAll()

        val user1 = userRepository.save(User(null, "userName", "password", "firstName", "lastName", true, Profile.USER))
        val user1Id = user1.id!!
        val user2 = userRepository.save(User(null, "userName", "password", "firstName", "lastName", true, Profile.USER))
        val user2Id = user2.id!!
        val user3 = userRepository.save(User(null, "userName", "password", "firstName", "lastName", true, Profile.USER))
        val user3Id = user3.id!!


        // Prepare a group and some users
        val group = groupRepository.save(Group(null, "groupName", setOf(user1Id, user2Id), setOf(user1Id, user2Id, user3Id)))
        val groupId = group.id!!


        // Values sent by the message
        var propagatedGroupId: Long? = null

        // Receive the events
        val applicationEventMulticaster = applicationContext.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME) as ApplicationEventMulticaster
        val listener = fun(event: ApplicationEvent) {
            when (event) {
                is GroupUpdatedEvent -> propagatedGroupId = event.groupId
            }
        }
        applicationEventMulticaster.addApplicationListener(listener)

        // Update the users of the group
        groupService.removeUserFromGroup(group, user3)

        // Check group was saved
        val groupUpdated1 = groupRepository.findById(groupId).get()
        Assertions.assertNotNull(groupUpdated1)
        Assertions.assertEquals(2, groupUpdated1.administratorIds.size)
        Assertions.assertTrue(groupUpdated1.administratorIds.contains(user1Id))
        Assertions.assertTrue(groupUpdated1.administratorIds.contains(user2Id))
        Assertions.assertEquals(2, groupUpdated1.userIds.size)
        Assertions.assertTrue(groupUpdated1.userIds.contains(user1Id))
        Assertions.assertTrue(groupUpdated1.userIds.contains(user2Id))

        // Check that a message was sent
        Assertions.assertEquals(groupId, propagatedGroupId)
        propagatedGroupId = null

        // Update the users and the admin of the group
        groupService.removeUserFromGroup(groupUpdated1, user2)

        // Check group was saved
        val groupUpdated2 = groupRepository.findById(groupId).get()
        Assertions.assertNotNull(groupUpdated2)
        Assertions.assertEquals(1, groupUpdated2.administratorIds.size)
        Assertions.assertTrue(groupUpdated2.administratorIds.contains(user1Id))
        Assertions.assertEquals(1, groupUpdated2.userIds.size)
        Assertions.assertTrue(groupUpdated2.userIds.contains(user1Id))

        // Check that a message was sent
        Assertions.assertEquals(groupId, propagatedGroupId)
        propagatedGroupId = null

        // Try to update the users and the admin of the group
        Assertions.assertThrows(ConstraintException::class.java) { groupService.removeUserFromGroup(groupUpdated2, user1) }

        applicationEventMulticaster.removeApplicationListener(listener)

        // Check group was not saved
        val groupUpdated3 = groupRepository.findById(groupId).get()
        Assertions.assertNotNull(groupUpdated3)
        Assertions.assertEquals(1, groupUpdated3.administratorIds.size)
        Assertions.assertTrue(groupUpdated3.administratorIds.contains(user1Id))
        Assertions.assertEquals(1, groupUpdated3.userIds.size)
        Assertions.assertTrue(groupUpdated3.userIds.contains(user1Id))

        // Check that a message was sent
        Assertions.assertNull(propagatedGroupId)
    }

    @Test
    fun `Delete group should delete group and clean users`() {

        groupRepository.deleteAll()
        userRepository.deleteAll()

        val user1 = userRepository.save(User(null, "userName", "password", "firstName", "lastName", true, Profile.USER))
        val user1Id = user1.id!!
        val user2 = userRepository.save(User(null, "userName", "password", "firstName", "lastName", true, Profile.USER))
        val user2Id = user2.id!!


        // Prepare a group and some users
        val group = groupRepository.save(Group(null, "groupName", setOf(user1Id), setOf(user1Id, user2Id)))
        val groupId = group.id!!

        // Values sent by the message
        var propagatedGroupId: Long? = null

        // Receive the events
        val applicationEventMulticaster = applicationContext.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME) as ApplicationEventMulticaster
        val listener = fun(event: ApplicationEvent) {
            when (event) {
                is GroupDeletedEvent -> propagatedGroupId = event.groupId
            }
        }
        applicationEventMulticaster.addApplicationListener(listener)

        // Update the users of the group
        groupService.deleteGroup(group)

        applicationEventMulticaster.removeApplicationListener(listener)

        // Check users are no more related to group deleted
        val groups1 = groupRepository.findAllByUserId(user1Id)
        Assertions.assertNotNull(groups1)
        Assertions.assertEquals(0, groups1.size)

        val groups2 = groupRepository.findAllByUserId(user1Id)
        Assertions.assertNotNull(groups2)
        Assertions.assertEquals(0, groups2.size)

        // Check that a message was sent
        Assertions.assertEquals(groupId, propagatedGroupId)
    }
}