package net.wuillemin.jds.common.service

import net.wuillemin.jds.common.config.CommonConfigDataBaseTest
import net.wuillemin.jds.common.entity.Group
import net.wuillemin.jds.common.entity.Profile
import net.wuillemin.jds.common.entity.User
import net.wuillemin.jds.common.event.GroupUpdatedEvent
import net.wuillemin.jds.common.event.UserCreatedEvent
import net.wuillemin.jds.common.event.UserDeletedEvent
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
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [
    CommonConfigDataBaseTest::class,
    UserService::class])
class UserServiceTest {

    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var groupRepository: GroupRepository
    @Autowired
    private lateinit var userService: UserService
    @Autowired
    private lateinit var applicationContext: ConfigurableApplicationContext


    @Test
    fun `Get group by name should work only if a single object is present`() {

        // Check found if one group
        userRepository.deleteAll()
        userRepository.save(User("userId", "userName", "password", "firstName", "lastName", true, Profile.USER, setOf("otherGroupId")))
        val result1 = userService.getUserByUserName("userName")
        Assertions.assertNotNull(result1)

        // Check exception if not found
        userRepository.deleteAll()
        Assertions.assertThrows(NotFoundException::class.java) { userService.getUserByUserName("userName") }

        // Check constraint error if multiple
        userRepository.deleteAll()
        userRepository.save(User("userId1", "userName", "password", "firstName", "lastName", true, Profile.USER, setOf("otherGroupId")))
        userRepository.save(User("userId2", "userName", "password", "firstName", "lastName", true, Profile.USER, setOf("otherGroupId")))
        Assertions.assertThrows(CriticalConstraintException::class.java) { userService.getUserByUserName("userName") }
    }

    @Test
    fun `Create safe user should give only store safe values`() {

        userRepository.deleteAll()
        val userToCreate = User(null, "userName", "password", "firstName", "lastName", false, Profile.ADMIN, emptySet())

        // Values sent by the message
        var propagatedUserId: String? = null

        // Receive the events
        val applicationEventMulticaster = applicationContext.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME) as ApplicationEventMulticaster
        val listener = fun(event: ApplicationEvent) {
            when (event) {
                is UserCreatedEvent -> propagatedUserId = event.userId
            }
        }
        applicationEventMulticaster.addApplicationListener(listener)

        // User does not exist
        val createdUser = userService.addSafeUser(userToCreate)

        applicationEventMulticaster.removeApplicationListener(listener)

        val userSaved = userRepository.findById(createdUser.id!!).get()
        Assertions.assertNotNull(userSaved)
        Assertions.assertEquals("userName", userSaved.userName)
        Assertions.assertEquals("firstName", userSaved.firstName)
        Assertions.assertEquals("lastName", userSaved.lastName)
        Assertions.assertNotEquals("password", userSaved.password)
        Assertions.assertTrue(userSaved.enabled)
        Assertions.assertEquals(Profile.USER, userSaved.profile)
        Assertions.assertTrue(userSaved.participatingGroupIds.isEmpty())

        // Check that a message was not sent
        Assertions.assertNotNull(propagatedUserId)
    }

    @Test
    fun `Create a user can only be done without groups`() {

        userRepository.deleteAll()
        val userToCreate = User(null, "userName", "password", "firstName", "lastName", true, Profile.USER, setOf("groupId"))

        // Values sent by the message
        var propagatedUserId: String? = null

        // Receive the events
        val applicationEventMulticaster = applicationContext.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME) as ApplicationEventMulticaster
        val listener = fun(event: ApplicationEvent) {
            when (event) {
                is UserCreatedEvent -> propagatedUserId = event.userId
            }
        }
        applicationEventMulticaster.addApplicationListener(listener)

        // User does not exist
        Assertions.assertThrows(BadParameterException::class.java) { userService.addSafeUser(userToCreate) }

        applicationEventMulticaster.removeApplicationListener(listener)

        // Check that no message was not sent
        Assertions.assertNull(propagatedUserId)
    }

    @Test
    fun `Create a user can only be done if userName is unique`() {

        userRepository.deleteAll()
        userRepository.save(User(null, "userName", "a", "b", "c", true, Profile.USER, setOf("otherGroupId")))

        val userToCreate = User(null, "userName", "password", "firstName", "lastName", true, Profile.USER, emptySet())

        // Values sent by the message
        var propagatedUserId: String? = null

        // Receive the events
        val applicationEventMulticaster = applicationContext.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME) as ApplicationEventMulticaster
        val listener = fun(event: ApplicationEvent) {
            when (event) {
                is UserCreatedEvent -> propagatedUserId = event.userId
            }
        }
        applicationEventMulticaster.addApplicationListener(listener)

        // User does not exist
        Assertions.assertThrows(ConstraintException::class.java) { userService.addSafeUser(userToCreate) }

        applicationEventMulticaster.removeApplicationListener(listener)

        // Check that no message was not sent
        Assertions.assertNull(propagatedUserId)
    }


    @Test
    fun `Update safe user should give only store safe values`() {

        userRepository.deleteAll()
        val user = userRepository.save(User(null, "a", "b", "c", "d", false, Profile.USER, setOf("groupId")))

        val userToUpdate = User(user.id, "userName", "password", "firstName", "lastName", true, Profile.ADMIN, setOf("groupId"))

        // Values sent by the message
        var propagatedUserId: String? = null

        // Receive the events
        val applicationEventMulticaster = applicationContext.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME) as ApplicationEventMulticaster
        val listener = fun(event: ApplicationEvent) {
            when (event) {
                is UserUpdatedEvent -> propagatedUserId = event.userId
            }
        }
        applicationEventMulticaster.addApplicationListener(listener)

        // User does not exist
        val updatedUser = userService.updateSafeUser(userToUpdate)

        applicationEventMulticaster.removeApplicationListener(listener)

        val userSaved = userRepository.findById(updatedUser.id!!).get()
        Assertions.assertNotNull(userSaved)
        Assertions.assertEquals("userName", userSaved.userName)
        Assertions.assertEquals("firstName", userSaved.firstName)
        Assertions.assertEquals("lastName", userSaved.lastName)
        Assertions.assertNotEquals("password", userSaved.password)
        Assertions.assertFalse(userSaved.enabled)
        Assertions.assertEquals(Profile.USER, userSaved.profile)
        Assertions.assertEquals(setOf("groupId"), userSaved.participatingGroupIds)

        // Check that a message was not sent
        Assertions.assertNotNull(propagatedUserId)
    }

    @Test
    fun `Update unsafe user should give store unsafe values`() {

        userRepository.deleteAll()
        val user = userRepository.save(User(null, "a", "b", "c", "d", false, Profile.USER, setOf("groupId")))

        val userToUpdate = User(user.id, "userName", "password", "firstName", "lastName", true, Profile.ADMIN, setOf("groupId"))

        // Values sent by the message
        var propagatedUserId: String? = null

        // Receive the events
        val applicationEventMulticaster = applicationContext.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME) as ApplicationEventMulticaster
        val listener = fun(event: ApplicationEvent) {
            when (event) {
                is UserUpdatedEvent -> propagatedUserId = event.userId
            }
        }
        applicationEventMulticaster.addApplicationListener(listener)

        // Update user
        val updatedUser = userService.updateUserAllOptions(userToUpdate)

        applicationEventMulticaster.removeApplicationListener(listener)

        val userSaved = userRepository.findById(updatedUser.id!!).get()
        Assertions.assertNotNull(userSaved)
        Assertions.assertEquals("userName", userSaved.userName)
        Assertions.assertEquals("firstName", userSaved.firstName)
        Assertions.assertEquals("lastName", userSaved.lastName)
        Assertions.assertNotEquals("password", userSaved.password)
        Assertions.assertTrue(userSaved.enabled)
        Assertions.assertEquals(Profile.ADMIN, userSaved.profile)
        Assertions.assertEquals(setOf("groupId"), userSaved.participatingGroupIds)

        // Check that a message was not sent
        Assertions.assertNotNull(propagatedUserId)
    }

    @Test
    fun `Update a user can not change its group`() {

        userRepository.deleteAll()
        val user = userRepository.save(User(null, "userName", "password", "firstName", "lastName", true, Profile.USER, setOf("groupId")))

        // Values sent by the message
        var propagatedUserId: String? = null

        // Receive the events
        val applicationEventMulticaster = applicationContext.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME) as ApplicationEventMulticaster
        val listener = fun(event: ApplicationEvent) {
            when (event) {
                is UserUpdatedEvent -> propagatedUserId = event.userId
            }
        }
        applicationEventMulticaster.addApplicationListener(listener)

        // Update user
        Assertions.assertThrows(BadParameterException::class.java) { userService.updateSafeUser(user.copy(participatingGroupIds = emptySet())) }

        applicationEventMulticaster.removeApplicationListener(listener)

        // Check that no message was not sent
        Assertions.assertNull(propagatedUserId)
    }

    @Test
    fun `Create a userName can only be done if userName is unique`() {
        userRepository.deleteAll()
        userRepository.save(User(null, "existing", "a", "b", "c", true, Profile.USER, setOf("groupId")))

        val user = userRepository.save(User(null, "userName", "password", "firstName", "lastName", true, Profile.USER, setOf("groupId")))

        // Values sent by the message
        var propagatedUserId: String? = null

        // Receive the events
        val applicationEventMulticaster = applicationContext.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME) as ApplicationEventMulticaster
        val listener = fun(event: ApplicationEvent) {
            when (event) {
                is UserUpdatedEvent -> propagatedUserId = event.userId
            }
        }
        applicationEventMulticaster.addApplicationListener(listener)

        // Update user
        Assertions.assertThrows(ConstraintException::class.java) { userService.updateSafeUser(user.copy(userName = "existing")) }

        applicationEventMulticaster.removeApplicationListener(listener)

        // Check that no message was not sent
        Assertions.assertNull(propagatedUserId)
    }

    @Test
    fun `User can be deleted`() {
        userRepository.deleteAll()
        val user1 = userRepository.save(User("userId1", "userName1", "a", "b", "c", true, Profile.USER, setOf("groupId")))
        val user2 = userRepository.save(User("userId2", "userName2", "a", "b", "c", true, Profile.USER, setOf("groupId")))
        groupRepository.save(Group("groupId", "groupName", setOf("userId1", "userId2"), setOf("userId1", "userId2")))

        // Values sent by the message
        var propagatedUserId: String? = null
        var propagatedGroupId: String? = null

        // Receive the events
        val applicationEventMulticaster = applicationContext.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME) as ApplicationEventMulticaster
        val listener = fun(event: ApplicationEvent) {
            when (event) {
                is UserDeletedEvent  -> propagatedUserId = event.userId
                is GroupUpdatedEvent -> propagatedGroupId = event.groupId
            }
        }
        applicationEventMulticaster.addApplicationListener(listener)

        // Delete user
        userService.deleteUser(user1)

        // Check group was saved
        val groupUpdated = groupRepository.findById("groupId").get()
        Assertions.assertNotNull(groupUpdated)
        Assertions.assertEquals(1, groupUpdated.administratorIds.size)
        Assertions.assertTrue(groupUpdated.userIds.contains("userId2"))
        Assertions.assertEquals(1, groupUpdated.userIds.size)
        Assertions.assertTrue(groupUpdated.userIds.contains("userId2"))

        // Check that no message was not sent
        Assertions.assertEquals("userId1", propagatedUserId)
        Assertions.assertEquals("groupId", propagatedGroupId)

        // Delete user should not be possible
        Assertions.assertThrows(ConstraintException::class.java) { userService.deleteUser(user2) }

        applicationEventMulticaster.removeApplicationListener(listener)
    }
}