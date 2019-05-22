package net.wuillemin.jds.dataserver.repository

import net.wuillemin.jds.dataserver.config.DataServerConfigDataBaseTest
import net.wuillemin.jds.dataserver.entity.model.SchemaGSheet
import net.wuillemin.jds.dataserver.entity.model.SchemaSQL
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
class SchemaRepositoryTest {

    @Autowired
    private lateinit var serverRepository: ServerRepository
    @Autowired
    private lateinit var schemaRepository: SchemaRepository

    @Test
    fun `Can do basic operations`() {

        schemaRepository.deleteAll()

        //
        // Test queries on empty repository
        //

        Assertions.assertEquals(0, schemaRepository.count())

        Assertions.assertEquals(0, schemaRepository.findAll().toList().size)
        Assertions.assertEquals(0, schemaRepository.findAllById(listOf(1, 2)).size)
        Assertions.assertEquals(0, schemaRepository.findAllByServerId(1).size)
        Assertions.assertEquals(0, schemaRepository.findAllByServerIdIn(listOf(1, 2)).size)

        Assertions.assertFalse(schemaRepository.existsById(1))

        Assertions.assertFalse(schemaRepository.findById(1).isPresent)

        //
        // Create some servers
        //

        val server1 = serverRepository.save(ServerSQL(null, "Server1", 1, true, "url", "user", "password", "driver"))
        val server1Id = server1.id!!
        val server2 = serverRepository.save(ServerSQL(null, "Server2", 1, true, "url", "user", "password", "driver"))
        val server2Id = server2.id!!
        val server3 = serverRepository.save(ServerSQL(null, "Server3", 1, true, "url", "user", "password", "driver"))
        val server3Id = server3.id!!

        //
        // Create some schemas
        //

        val schema1 = schemaRepository.save(SchemaSQL(null, "schema1", server1Id, "role1"))
        val schema1Id = schema1.id!!

        val schemas = schemaRepository.saveAll(listOf(
            SchemaSQL(null, "schema2", server1Id, "role2"),
            SchemaGSheet(null, "schema3", server2Id),
            SchemaGSheet(null, "schema4", server3Id)))


        //
        // Test queries on filled repository
        //

        Assertions.assertEquals(4, schemaRepository.count())

        Assertions.assertEquals(4, schemaRepository.findAll().toList().size)
        Assertions.assertEquals(1, schemaRepository.findAllById(listOf(schema1Id)).toList().size)
        Assertions.assertEquals(2, schemaRepository.findAllByServerId(server1Id).size)
        Assertions.assertEquals(3, schemaRepository.findAllByServerIdIn(listOf(server1Id, server2Id)).size)

        Assertions.assertTrue(schemaRepository.existsById(schema1Id))

        Assertions.assertTrue(schemaRepository.findById(schema1Id).isPresent)
        Assertions.assertEquals("schema1", schema1.name)
        Assertions.assertEquals(server1Id, schema1.serverId)
        Assertions.assertEquals("role1", schema1.roleName)

        val schema3 = schemas.first { it.name == "schema3" }

        Assertions.assertEquals("schema3", schema3.name)
        Assertions.assertEquals(server2Id, schema3.serverId)
        Assertions.assertTrue(schema3 is SchemaGSheet)

        //
        // Update existing schema sql -> gsheet
        //

        val schema1Updated = schemaRepository.save(SchemaGSheet(schema1Id, "schema5", server3Id))

        Assertions.assertEquals(4, schemaRepository.count())
        Assertions.assertTrue(schemaRepository.findById(schema1Id).isPresent)
        Assertions.assertEquals("schema5", schema1Updated.name)
        Assertions.assertEquals(server3Id, schema1Updated.serverId)

        //
        // Update existing schema gsheet -> sql
        //

        val schema1Updated2 = schemaRepository.save(SchemaSQL(schema1Id, "schema1", server1Id, "role10"))

        Assertions.assertEquals(4, schemaRepository.count())
        Assertions.assertTrue(schemaRepository.findById(schema1Id).isPresent)
        Assertions.assertEquals("schema1", schema1Updated2.name)
        Assertions.assertEquals(server1Id, schema1Updated2.serverId)
        Assertions.assertEquals("role10", schema1Updated2.roleName)

        //
        // Update non existing schema
        //

        val schema6Updated = schemaRepository.save(SchemaGSheet(1000000, "schema6", server3Id))
        val schema6Id = schema6Updated.id!!

        Assertions.assertEquals(5, schemaRepository.count())
        Assertions.assertTrue(schemaRepository.findById(schema6Id).isPresent)
        Assertions.assertEquals("schema6", schema6Updated.name)
        Assertions.assertEquals(server3Id, schema6Updated.serverId)

        // Delete single schema
        schemaRepository.deleteById(schema1Id)
        Assertions.assertEquals(4, schemaRepository.count())

        schemaRepository.delete(schema6Updated)
        Assertions.assertEquals(3, schemaRepository.count())

        // Delete multiple schemas
        schemaRepository.deleteAll(listOf(schemas.first()))
        Assertions.assertEquals(2, schemaRepository.count())

        schemaRepository.deleteAll()
        Assertions.assertEquals(0, schemaRepository.count())
    }
}