package net.wuillemin.jds.dataserver.repository

import net.wuillemin.jds.dataserver.config.DataServerConfigDataBaseTest
import net.wuillemin.jds.dataserver.entity.model.ServerGSheet
import net.wuillemin.jds.dataserver.entity.model.ServerSQL
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [
    DataServerConfigDataBaseTest::class])
class ServerRepositoryTest {

    @Autowired
    private lateinit var serverRepository: ServerRepository

    @Test
    fun `Can do basic operations`() {

        serverRepository.deleteAll()

        //
        // Test queries on empty repository
        //
        Assertions.assertEquals(0, serverRepository.count())

        Assertions.assertEquals(0, serverRepository.findAll().toList().size)
        Assertions.assertEquals(0, serverRepository.findAllById(listOf(1, 2)).toList().size)
        Assertions.assertEquals(0, serverRepository.findAllByGroupIdIn(listOf(1, 2)).toList().size)

        Assertions.assertFalse(serverRepository.existsById(1))

        Assertions.assertFalse(serverRepository.findById(1).isPresent)

        //
        // Create some servers
        //

        val server1 = serverRepository.save(ServerSQL(null, "Server1", 1, true, "url1", "user1", "password1", "driver1"))
        val server1Id = server1.id!!

        val servers = serverRepository.saveAll(listOf(
            ServerSQL(null, "Server2", 1, true, "url2", "user2", "password2", "driver2"),
            ServerGSheet(null, "Server3", 3, false, "url3", "user3", "password3"),
            ServerGSheet(null, "Server4", 4, true, "url4", "user4", "password4")))


        //
        // Test queries on filled repository
        //

        Assertions.assertEquals(4, serverRepository.count())

        Assertions.assertEquals(4, serverRepository.findAll().toList().size)
        Assertions.assertEquals(1, serverRepository.findAllById(listOf(server1Id)).toList().size)
        Assertions.assertEquals(3, serverRepository.findAllByGroupIdIn(listOf(1, 3)).toList().size)

        Assertions.assertTrue(serverRepository.existsById(server1Id))

        Assertions.assertTrue(serverRepository.findById(server1Id).isPresent)
        Assertions.assertEquals("Server1", server1.name)
        Assertions.assertEquals(1, server1.groupId)
        Assertions.assertTrue(server1.customerDefined)
        Assertions.assertEquals("url1", server1.jdbcURL)
        Assertions.assertEquals("user1", server1.userName)
        Assertions.assertEquals("password1", server1.password)
        Assertions.assertEquals("driver1", server1.driverClassName)

        val server3 = servers.first { it.name == "Server3" }

        Assertions.assertEquals("Server3", server3.name)
        Assertions.assertEquals(3, server3.groupId)
        Assertions.assertFalse(server3.customerDefined)

        when (server3) {
            is ServerGSheet -> {
                Assertions.assertEquals("url3", server3.workbookURL)
                Assertions.assertEquals("user3", server3.userName)
                Assertions.assertEquals("password3", server3.password)
            }
            else         -> Assertions.fail()
        }

        //
        // Update existing server
        //

        val server1Updated = serverRepository.save(ServerGSheet(server1Id, "Server5", 5, false, "url5", "user5", "password5"))

        Assertions.assertEquals(4, serverRepository.count())
        Assertions.assertTrue(serverRepository.findById(server1Id).isPresent)
        Assertions.assertEquals("Server5", server1Updated.name)
        Assertions.assertEquals(5, server1Updated.groupId)
        Assertions.assertFalse(server1Updated.customerDefined)
        Assertions.assertEquals("url5", server1Updated.workbookURL)
        Assertions.assertEquals("user5", server1Updated.userName)
        Assertions.assertEquals("password5", server1Updated.password)

        //
        // Update non existing server
        //

        val server6Updated = serverRepository.save(ServerGSheet(1000000, "Server6", 6, false, "url6", "user6", "password6"))
        val server6Id = server6Updated.id!!

        Assertions.assertEquals(5, serverRepository.count())
        Assertions.assertTrue(serverRepository.findById(server6Id).isPresent)
        Assertions.assertEquals("Server6", server6Updated.name)
        Assertions.assertEquals(6, server6Updated.groupId)
        Assertions.assertFalse(server6Updated.customerDefined)
        Assertions.assertEquals("url6", server6Updated.workbookURL)
        Assertions.assertEquals("user6", server6Updated.userName)
        Assertions.assertEquals("password6", server6Updated.password)

        // Delete single server
        serverRepository.deleteById(server1Id)
        Assertions.assertEquals(4, serverRepository.count())

        serverRepository.delete(server6Updated)
        Assertions.assertEquals(3, serverRepository.count())

        // Delete multiple servers
        serverRepository.deleteAll(listOf(servers.first()))
        Assertions.assertEquals(2, serverRepository.count())

        serverRepository.deleteAll()
        Assertions.assertEquals(0, serverRepository.count())
    }
}