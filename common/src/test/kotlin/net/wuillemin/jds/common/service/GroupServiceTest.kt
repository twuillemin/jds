package net.wuillemin.jds.common.service

import net.wuillemin.jds.common.config.CommonConfigDataBaseTest
import net.wuillemin.jds.common.entity.Group
import net.wuillemin.jds.common.entity.Profile
import net.wuillemin.jds.common.entity.User
import net.wuillemin.jds.common.event.GroupCreatedEvent
import net.wuillemin.jds.common.event.GroupDeletedEvent
import net.wuillemin.jds.common.event.GroupUpdatedEvent
import net.wuillemin.jds.common.event.UserUpdatedEvent
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
        groupRepository.save(Group("groupId", "azerty", emptySet(), emptySet()))
        val result1 = groupService.getGroupByName("azerty")
        Assertions.assertNotNull(result1)

        // Check exception if not found
        groupRepository.deleteAll()
        Assertions.assertThrows(NotFoundException::class.java) { groupService.getGroupByName("azerty") }

        // Check constraint error if multiple
        groupRepository.deleteAll()
        groupRepository.save(Group("groupId1", "azerty", emptySet(), emptySet()))
        groupRepository.save(Group("groupId2", "azerty", emptySet(), emptySet()))
        Assertions.assertThrows(CriticalConstraintException::class.java) { groupService.getGroupByName("azerty") }
    }

    @Test
    fun `Create group should save a new group`() {

        val groupToCreate = Group(null, "groupName", emptySet(), emptySet())
        val userCreating = User("userId", "userName", "password", "firstName", "lastName", true, Profile.USER, setOf("otherGroupId"))

        // Group does not exist before
        groupRepository.deleteAll()
        userRepository.save(userCreating)

        // Values sent by the message
        var propagatedGroupId: String? = null
        val userUpdatedIds = ArrayList<String>()

        // Receive the events
        val applicationEventMulticaster = applicationContext.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME) as ApplicationEventMulticaster
        val listener = fun(event: ApplicationEvent) {
            when (event) {
                is GroupCreatedEvent -> propagatedGroupId = event.groupId
                is UserUpdatedEvent  -> userUpdatedIds.add(event.userId)
            }
        }
        applicationEventMulticaster.addApplicationListener(listener)

        // Save the group
        val savedGroup = groupService.addGroup(groupToCreate, userCreating)

        // Clean receiving the events
        applicationEventMulticaster.removeApplicationListener(listener)

        // Check group was saved
        val group = groupRepository.findById(savedGroup.id!!).get()
        Assertions.assertNotNull(group)
        Assertions.assertEquals("groupName", group.name)
        Assertions.assertTrue(group.administratorIds.contains("userId"))
        Assertions.assertTrue(group.administratorIds.contains("userId"))

        // Check user was updated
        val user = userRepository.findById(userCreating.id!!).get()
        Assertions.assertNotNull(user)
        Assertions.assertTrue(user.participatingGroupIds.contains("otherGroupId"))
        Assertions.assertTrue(user.participatingGroupIds.contains(group.id))

        // Check that a message was sent
        Assertions.assertNotNull(propagatedGroupId)
        Assertions.assertEquals(1, userUpdatedIds.size)
        Assertions.assertTrue(userUpdatedIds.contains("userId"))
    }

    @Test
    fun `Create group should not save a new group if bad params`() {

        groupRepository.deleteAll()

        // Group is already persisted
        val groupToCreate1 = Group("groupId", "groupName", emptySet(), emptySet())
        val userCreating1 = User("userId", "userName", "password", "firstName", "lastName", true, Profile.USER, emptySet())
        Assertions.assertThrows(BadParameterException::class.java) { groupService.addGroup(groupToCreate1, userCreating1) }

        // User is not persisted
        val groupToCreate2 = Group("groupId", "groupName", emptySet(), emptySet())
        val userCreating2 = User("userId", "userName", "password", "firstName", "lastName", true, Profile.USER, emptySet())
        Assertions.assertThrows(BadParameterException::class.java) { groupService.addGroup(groupToCreate2, userCreating2) }

        // Group name already exist
        val groupToCreate3 = Group(null, "groupName", emptySet(), emptySet())
        val userCreating3 = User("userId", "userName", "password", "firstName", "lastName", true, Profile.USER, emptySet())
        groupRepository.save(Group("groupId", "groupName", emptySet(), emptySet()))
        Assertions.assertThrows(ConstraintException::class.java) { groupService.addGroup(groupToCreate3, userCreating3) }
    }

    @Test
    fun `Set group users should update users`() {

        groupRepository.deleteAll()
        userRepository.deleteAll()

        // Prepare a group and some users
        val group = groupRepository.save(Group("groupId", "groupName", setOf("adminToStayInGroupId"), setOf("userToBeRemovedFromGroupId", "userToStayInGroupId", "adminToStayInGroupId")))
        val userToBeAddedToGroup = userRepository.save(User("userToBeAddedToGroupId", "userName", "password", "firstName", "lastName", true, Profile.USER, setOf("otherGroupId")))
        val userToStayInGroup = userRepository.save(User("userToStayInGroupId", "userName", "password", "firstName", "lastName", true, Profile.USER, setOf("groupId", "otherGroupId")))
        // This user will stay in group as not removed
        userRepository.save(User("userToBeRemovedFromGroupId", "userName", "password", "firstName", "lastName", true, Profile.USER, setOf("groupId", "otherGroupId")))
        // This user will stay in group but just as admin
        userRepository.save(User("adminToStayInGroupId", "userName", "password", "firstName", "lastName", true, Profile.USER, setOf("groupId")))

        // Values sent by the message
        var propagatedGroupId: String? = null
        val userUpdatedIds = ArrayList<String>()

        // Receive the events
        val applicationEventMulticaster = applicationContext.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME) as ApplicationEventMulticaster
        val listener = fun(event: ApplicationEvent) {
            when (event) {
                is GroupUpdatedEvent -> propagatedGroupId = event.groupId
                is UserUpdatedEvent  -> userUpdatedIds.add(event.userId)
            }
        }
        applicationEventMulticaster.addApplicationListener(listener)

        // Update the users of the group
        groupService.setGroupUsers(group, listOf(userToBeAddedToGroup, userToStayInGroup))

        applicationEventMulticaster.removeApplicationListener(listener)

        // Check group was saved
        val groupUpdated = groupRepository.findById("groupId").get()
        Assertions.assertNotNull(groupUpdated)
        Assertions.assertEquals(1, groupUpdated.administratorIds.size)
        Assertions.assertTrue(groupUpdated.userIds.contains("adminToStayInGroupId"))
        Assertions.assertEquals(3, groupUpdated.userIds.size)
        Assertions.assertTrue(groupUpdated.userIds.contains("userToBeAddedToGroupId"))
        Assertions.assertTrue(groupUpdated.userIds.contains("userToStayInGroupId"))
        Assertions.assertTrue(groupUpdated.userIds.contains("adminToStayInGroupId"))

        // Check users to be added was updated
        val userAdded = userRepository.findById("userToBeAddedToGroupId").get()
        Assertions.assertNotNull(userAdded)
        Assertions.assertEquals(2, userAdded.participatingGroupIds.size)
        Assertions.assertTrue(userAdded.participatingGroupIds.contains("otherGroupId"))
        Assertions.assertTrue(userAdded.participatingGroupIds.contains("groupId"))

        // Check users to be removed was updated
        val userRemoved = userRepository.findById("userToBeRemovedFromGroupId").get()
        Assertions.assertNotNull(userRemoved)
        Assertions.assertEquals(1, userRemoved.participatingGroupIds.size)
        Assertions.assertTrue(userRemoved.participatingGroupIds.contains("otherGroupId"))

        // Check users to stay is still here
        val userStaying = userRepository.findById("userToStayInGroupId").get()
        Assertions.assertNotNull(userStaying)
        Assertions.assertEquals(2, userStaying.participatingGroupIds.size)
        Assertions.assertTrue(userStaying.participatingGroupIds.contains("groupId"))
        Assertions.assertTrue(userStaying.participatingGroupIds.contains("otherGroupId"))

        // Check admin was not removed
        val adminStaying = userRepository.findById("adminToStayInGroupId").get()
        Assertions.assertNotNull(userStaying)
        Assertions.assertEquals(1, adminStaying.participatingGroupIds.size)
        Assertions.assertTrue(adminStaying.participatingGroupIds.contains("groupId"))

        // Check that a message was sent
        Assertions.assertEquals("groupId", propagatedGroupId)
        Assertions.assertEquals(2, userUpdatedIds.size)
        Assertions.assertTrue(userUpdatedIds.contains("userToBeRemovedFromGroupId"))
        Assertions.assertTrue(userUpdatedIds.contains("userToBeAddedToGroupId"))
    }

    @Test
    fun `Set group administrators should update administrators`() {

        groupRepository.deleteAll()
        userRepository.deleteAll()

        // Prepare a group and some users
        val group = groupRepository.save(Group("groupId", "groupName", setOf("userToBeRemovedFromGroupId", "userToStayInGroupId"), setOf("userToBeRemovedFromGroupId", "userToStayInGroupId", "otherUserId")))
        val adminToBeAddedToGroup = userRepository.save(User("userToBeAddedToGroupId", "userName", "password", "firstName", "lastName", true, Profile.USER, setOf("otherGroupId")))
        val adminToStayInGroup = userRepository.save(User("userToStayInGroupId", "userName", "password", "firstName", "lastName", true, Profile.USER, setOf("groupId", "otherGroupId")))
        // This admin will stay in group as not removed
        userRepository.save(User("userToBeRemovedFromGroupId", "userName", "password", "firstName", "lastName", true, Profile.USER, setOf("groupId", "otherGroupId")))

        // Values sent by the message
        var propagatedGroupId: String? = null
        val userUpdatedIds = ArrayList<String>()

        // Receive the events
        val applicationEventMulticaster = applicationContext.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME) as ApplicationEventMulticaster
        val listener = fun(event: ApplicationEvent) {
            when (event) {
                is GroupUpdatedEvent -> propagatedGroupId = event.groupId
                is UserUpdatedEvent  -> userUpdatedIds.add(event.userId)
            }
        }
        applicationEventMulticaster.addApplicationListener(listener)

        // Update the users of the group
        groupService.setGroupAdministrators(group, listOf(adminToBeAddedToGroup, adminToStayInGroup))

        applicationEventMulticaster.removeApplicationListener(listener)

        // Check group was saved
        val groupUpdated = groupRepository.findById("groupId").get()
        Assertions.assertNotNull(groupUpdated)
        Assertions.assertEquals(2, groupUpdated.administratorIds.size)
        Assertions.assertTrue(groupUpdated.administratorIds.contains("userToBeAddedToGroupId"))
        Assertions.assertTrue(groupUpdated.administratorIds.contains("userToStayInGroupId"))
        Assertions.assertEquals(4, groupUpdated.userIds.size)
        Assertions.assertTrue(groupUpdated.userIds.contains("userToBeRemovedFromGroupId"))
        Assertions.assertTrue(groupUpdated.userIds.contains("userToBeAddedToGroupId"))
        Assertions.assertTrue(groupUpdated.userIds.contains("userToStayInGroupId"))
        Assertions.assertTrue(groupUpdated.userIds.contains("otherUserId"))

        // Check users to be added was updated
        val userAdded = userRepository.findById("userToBeAddedToGroupId").get()
        Assertions.assertNotNull(userAdded)
        Assertions.assertEquals(2, userAdded.participatingGroupIds.size)
        Assertions.assertTrue(userAdded.participatingGroupIds.contains("otherGroupId"))
        Assertions.assertTrue(userAdded.participatingGroupIds.contains("groupId"))

        // Check users to be removed was updated
        val userRemoved = userRepository.findById("userToBeRemovedFromGroupId").get()
        Assertions.assertNotNull(userRemoved)
        Assertions.assertEquals(1, userRemoved.participatingGroupIds.size)
        Assertions.assertTrue(userRemoved.participatingGroupIds.contains("otherGroupId"))

        // Check users to stay is still here
        val userStaying = userRepository.findById("userToStayInGroupId").get()
        Assertions.assertNotNull(userStaying)
        Assertions.assertEquals(2, userStaying.participatingGroupIds.size)
        Assertions.assertTrue(userStaying.participatingGroupIds.contains("groupId"))
        Assertions.assertTrue(userStaying.participatingGroupIds.contains("otherGroupId"))

        // Check that a message was sent
        Assertions.assertEquals("groupId", propagatedGroupId)
        Assertions.assertEquals(2, userUpdatedIds.size)
        Assertions.assertTrue(userUpdatedIds.contains("userToBeRemovedFromGroupId"))
        Assertions.assertTrue(userUpdatedIds.contains("userToBeAddedToGroupId"))
    }

    @Test
    fun `Remove user from group should remove the user but not the last one`() {
        groupRepository.deleteAll()
        userRepository.deleteAll()

        // Prepare a group and some users
        val group = groupRepository.save(Group("groupId", "groupName", setOf("userId1", "userId2"), setOf("userId1", "userId2", "userId3")))
        val user1 = userRepository.save(User("userId1", "userName", "password", "firstName", "lastName", true, Profile.USER, setOf("otherGroupId", "groupId")))
        val user2 = userRepository.save(User("userId2", "userName", "password", "firstName", "lastName", true, Profile.USER, setOf("otherGroupId", "groupId")))
        val user3 = userRepository.save(User("userId3", "userName", "password", "firstName", "lastName", true, Profile.USER, setOf("otherGroupId", "groupId")))

        // Values sent by the message
        var propagatedGroupId: String? = null
        val userUpdatedIds = ArrayList<String>()

        // Receive the events
        val applicationEventMulticaster = applicationContext.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME) as ApplicationEventMulticaster
        val listener = fun(event: ApplicationEvent) {
            when (event) {
                is GroupUpdatedEvent -> propagatedGroupId = event.groupId
                is UserUpdatedEvent  -> userUpdatedIds.add(event.userId)
            }
        }
        applicationEventMulticaster.addApplicationListener(listener)

        // Update the users of the group
        groupService.removeUserFromGroup(group, user3)

        // Check group was saved
        val groupUpdated1 = groupRepository.findById("groupId").get()
        Assertions.assertNotNull(groupUpdated1)
        Assertions.assertEquals(2, groupUpdated1.administratorIds.size)
        Assertions.assertTrue(groupUpdated1.administratorIds.contains("userId1"))
        Assertions.assertTrue(groupUpdated1.administratorIds.contains("userId2"))
        Assertions.assertEquals(2, groupUpdated1.userIds.size)
        Assertions.assertTrue(groupUpdated1.userIds.contains("userId1"))
        Assertions.assertTrue(groupUpdated1.userIds.contains("userId2"))

        // Check users to be added was updated
        val user3Updated = userRepository.findById("userId3").get()
        Assertions.assertNotNull(user3Updated)
        Assertions.assertEquals(1, user3Updated.participatingGroupIds.size)
        Assertions.assertTrue(user3Updated.participatingGroupIds.contains("otherGroupId"))

        // Check that a message was sent
        Assertions.assertEquals("groupId", propagatedGroupId)
        Assertions.assertEquals(1, userUpdatedIds.size)
        Assertions.assertTrue(userUpdatedIds.contains("userId3"))
        propagatedGroupId = null
        userUpdatedIds.clear()

        // Update the users and the admin of the group
        groupService.removeUserFromGroup(groupUpdated1, user2)

        // Check group was saved
        val groupUpdated2 = groupRepository.findById("groupId").get()
        Assertions.assertNotNull(groupUpdated2)
        Assertions.assertEquals(1, groupUpdated2.administratorIds.size)
        Assertions.assertTrue(groupUpdated2.administratorIds.contains("userId1"))
        Assertions.assertEquals(1, groupUpdated2.userIds.size)
        Assertions.assertTrue(groupUpdated2.userIds.contains("userId1"))

        // Check users to be added was updated
        val user2Updated = userRepository.findById("userId2").get()
        Assertions.assertNotNull(user2Updated)
        Assertions.assertEquals(1, user2Updated.participatingGroupIds.size)
        Assertions.assertTrue(user2Updated.participatingGroupIds.contains("otherGroupId"))

        // Check that a message was sent
        Assertions.assertEquals("groupId", propagatedGroupId)
        Assertions.assertEquals(1, userUpdatedIds.size)
        Assertions.assertTrue(userUpdatedIds.contains("userId2"))
        propagatedGroupId = null
        userUpdatedIds.clear()

        // Try to update the users and the admin of the group
        Assertions.assertThrows(ConstraintException::class.java) { groupService.removeUserFromGroup(groupUpdated2, user1) }

        applicationEventMulticaster.removeApplicationListener(listener)

        // Check group was not saved
        val groupUpdated3 = groupRepository.findById("groupId").get()
        Assertions.assertNotNull(groupUpdated3)
        Assertions.assertEquals(1, groupUpdated3.administratorIds.size)
        Assertions.assertTrue(groupUpdated3.administratorIds.contains("userId1"))
        Assertions.assertEquals(1, groupUpdated3.userIds.size)
        Assertions.assertTrue(groupUpdated3.userIds.contains("userId1"))

        // Check users to be added was updated
        val user1Updated = userRepository.findById("userId1").get()
        Assertions.assertNotNull(user1Updated)
        Assertions.assertEquals(2, user1Updated.participatingGroupIds.size)
        Assertions.assertTrue(user1Updated.participatingGroupIds.contains("otherGroupId"))
        Assertions.assertTrue(user1Updated.participatingGroupIds.contains("groupId"))

        // Check that a message was not sent
        Assertions.assertNull(propagatedGroupId)
        Assertions.assertTrue(userUpdatedIds.isEmpty())
    }

    @Test
    fun `Delete group should delete group and clean users`() {

        // Prepare a group and some users
        val group = groupRepository.save(Group("groupId", "groupName", setOf("userId1"), setOf("userId1", "userId2")))
        userRepository.save(User("userId1", "userName", "password", "firstName", "lastName", true, Profile.USER, setOf("otherGroupId", "groupId")))
        userRepository.save(User("userId2", "userName", "password", "firstName", "lastName", true, Profile.USER, setOf("otherGroupId", "groupId")))

        // Values sent by the message
        var propagatedGroupId: String? = null
        val userUpdatedIds = ArrayList<String>()

        // Receive the events
        val applicationEventMulticaster = applicationContext.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME) as ApplicationEventMulticaster
        val listener = fun(event: ApplicationEvent) {
            when (event) {
                is GroupDeletedEvent -> propagatedGroupId = event.groupId
                is UserUpdatedEvent  -> userUpdatedIds.add(event.userId)
            }
        }
        applicationEventMulticaster.addApplicationListener(listener)

        // Update the users of the group
        groupService.deleteGroup(group)

        applicationEventMulticaster.removeApplicationListener(listener)

        Assertions.assertEquals("groupId", propagatedGroupId)
        Assertions.assertEquals(2, userUpdatedIds.size)
        Assertions.assertTrue(userUpdatedIds.contains("userId1"))
        Assertions.assertTrue(userUpdatedIds.contains("userId2"))
    }
}