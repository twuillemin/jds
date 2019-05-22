package net.wuillemin.jds.common.service

import net.wuillemin.jds.common.config.CommonConfigDataBaseTest
import net.wuillemin.jds.common.entity.Group
import net.wuillemin.jds.common.entity.Profile
import net.wuillemin.jds.common.entity.User
import net.wuillemin.jds.common.event.GroupUpdatedEvent
import net.wuillemin.jds.common.event.UserCreatedEvent
import net.wuillemin.jds.common.event.UserDeletedEvent
import net.wuillemin.jds.common.event.UserUpdatedEvent
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
    UserService::class,
    GroupService::class])
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
        userRepository.save(User(null, "userName", "password", "firstName", "lastName", true, Profile.USER))
        val result1 = userService.getUserByUserName("userName")
        Assertions.assertNotNull(result1)

        // Check exception if not found
        userRepository.deleteAll()
        Assertions.assertThrows(NotFoundException::class.java) { userService.getUserByUserName("userName") }

        // Check constraint error if multiple
        userRepository.deleteAll()
        userRepository.save(User(null, "userName", "password", "firstName", "lastName", true, Profile.USER))
        userRepository.save(User(null, "userName", "password", "firstName", "lastName", true, Profile.USER))
        Assertions.assertThrows(CriticalConstraintException::class.java) { userService.getUserByUserName("userName") }
    }

    @Test
    fun `Create safe user should give only store safe values`() {

        userRepository.deleteAll()
        val userToCreate = User(null, "userName", "password", "firstName", "lastName", false, Profile.ADMIN)

        // Values sent by the message
        var propagatedUserId: Long? = null

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
        Assertions.assertEquals("userName", userSaved.name)
        Assertions.assertEquals("firstName", userSaved.firstName)
        Assertions.assertEquals("lastName", userSaved.lastName)
        Assertions.assertNotEquals("password", userSaved.password)
        Assertions.assertTrue(userSaved.enabled)
        Assertions.assertEquals(Profile.USER, userSaved.profile)

        // Check that a message was not sent
        Assertions.assertNotNull(propagatedUserId)
    }

    @Test
    fun `Create a user can only be done if userName is unique`() {

        userRepository.deleteAll()
        userRepository.save(User(null, "userName", "a", "b", "c", true, Profile.USER))

        val userToCreate = User(null, "userName", "password", "firstName", "lastName", true, Profile.USER)

        // Values sent by the message
        var propagatedUserId: Long? = null

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
        val user = userRepository.save(User(null, "a", "b", "c", "d", false, Profile.USER))

        val userToUpdate = User(user.id, "userName", "password", "firstName", "lastName", true, Profile.ADMIN)

        // Values sent by the message
        var propagatedUserId: Long? = null

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
        Assertions.assertEquals("userName", userSaved.name)
        Assertions.assertEquals("firstName", userSaved.firstName)
        Assertions.assertEquals("lastName", userSaved.lastName)
        Assertions.assertNotEquals("password", userSaved.password)
        Assertions.assertFalse(userSaved.enabled)
        Assertions.assertEquals(Profile.USER, userSaved.profile)

        // Check that a message was not sent
        Assertions.assertNotNull(propagatedUserId)
    }

    @Test
    fun `Update unsafe user should give store unsafe values`() {

        userRepository.deleteAll()
        val user = userRepository.save(User(null, "a", "b", "c", "d", false, Profile.USER))

        val userToUpdate = User(user.id, "userName", "password", "firstName", "lastName", true, Profile.ADMIN)

        // Values sent by the message
        var propagatedUserId: Long? = null

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
        Assertions.assertEquals("userName", userSaved.name)
        Assertions.assertEquals("firstName", userSaved.firstName)
        Assertions.assertEquals("lastName", userSaved.lastName)
        Assertions.assertNotEquals("password", userSaved.password)
        Assertions.assertTrue(userSaved.enabled)
        Assertions.assertEquals(Profile.ADMIN, userSaved.profile)

        // Check that a message was not sent
        Assertions.assertNotNull(propagatedUserId)
    }

    @Test
    fun `Create a userName can only be done if userName is unique`() {
        userRepository.deleteAll()
        userRepository.save(User(null, "existing", "a", "b", "c", true, Profile.USER))

        val user = userRepository.save(User(null, "userName", "password", "firstName", "lastName", true, Profile.USER))

        // Values sent by the message
        var propagatedUserId: Long? = null

        // Receive the events
        val applicationEventMulticaster = applicationContext.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME) as ApplicationEventMulticaster
        val listener = fun(event: ApplicationEvent) {
            when (event) {
                is UserUpdatedEvent -> propagatedUserId = event.userId
            }
        }
        applicationEventMulticaster.addApplicationListener(listener)

        // Update user
        Assertions.assertThrows(ConstraintException::class.java) { userService.updateSafeUser(user.copy(name = "existing")) }

        applicationEventMulticaster.removeApplicationListener(listener)

        // Check that no message was not sent
        Assertions.assertNull(propagatedUserId)
    }

    @Test
    fun `User can be deleted`() {

        userRepository.deleteAll()

        val user1 = userRepository.save(User(null, "userName1", "a", "b", "c", true, Profile.USER))
        val user1Id = user1.id!!
        val user2 = userRepository.save(User(null, "userName2", "a", "b", "c", true, Profile.USER))
        val user2Id = user2.id!!

        val group = groupRepository.save(Group(null, "groupName", setOf(user1Id, user2Id), setOf(user1Id, user2Id)))
        val groupId = group.id!!

        // Values sent by the message
        var propagatedUserId: Long? = null
        var propagatedGroupId: Long? = null

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
        val groupUpdated = groupRepository.findById(groupId).get()
        Assertions.assertNotNull(groupUpdated)
        Assertions.assertEquals(1, groupUpdated.administratorIds.size)
        Assertions.assertTrue(groupUpdated.userIds.contains(user2Id))
        Assertions.assertEquals(1, groupUpdated.userIds.size)
        Assertions.assertTrue(groupUpdated.userIds.contains(user2Id))

        // Check that no message was not sent
        Assertions.assertEquals(user1Id, propagatedUserId)
        Assertions.assertEquals(groupId, propagatedGroupId)

        // Delete user should not be possible
        Assertions.assertThrows(ConstraintException::class.java) { userService.deleteUser(user2) }

        applicationEventMulticaster.removeApplicationListener(listener)
    }
}