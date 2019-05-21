package net.wuillemin.jds.dataserver.repository

import net.wuillemin.jds.dataserver.config.DataServerConfigDataBaseTest
import net.wuillemin.jds.dataserver.entity.model.DataProviderSQL
import net.wuillemin.jds.dataserver.entity.model.DataSource
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
class DataSourceRepositoryTest {

    @Autowired
    private lateinit var serverRepository: ServerRepository
    @Autowired
    private lateinit var schemaRepository: SchemaRepository
    @Autowired
    private lateinit var dataProviderRepository: DataProviderRepository
    @Autowired
    private lateinit var dataSourceRepository: DataSourceRepository

    @Test
    fun `Can do basic operations`() {

        dataSourceRepository.deleteAll()

        //
        // Test queries on empty repository
        //

        Assertions.assertEquals(0, dataSourceRepository.count())

        Assertions.assertEquals(0, dataSourceRepository.findAll().toList().size)
        Assertions.assertEquals(0, dataSourceRepository.findAllById(listOf(1, 2)).size)
        Assertions.assertEquals(0, dataSourceRepository.findAllByDataProviderId(1).size)
        Assertions.assertEquals(0, dataSourceRepository.findAllByDataProviderIdIn(listOf(1, 2)).size)

        Assertions.assertFalse(dataSourceRepository.existsById(1))

        Assertions.assertFalse(dataSourceRepository.findById(1).isPresent)

        //
        // Create some server and schemas
        //

        val server1 = serverRepository.save(ServerSQL(null, "Server1", 1, true, "url", "user", "password", "driver"))
        val server1Id = server1.id!!
        val schema1 = schemaRepository.save(SchemaSQL(null, "schema1", server1Id, "role1"))
        val schema1Id = schema1.id!!
        val dataProvider1 = dataProviderRepository.save(DataProviderSQL(null, "dataProvider1", schema1Id, emptyList(), true, "query1"))
        val dataProvider1Id = dataProvider1.id!!
        val dataProvider2 = dataProviderRepository.save(DataProviderSQL(null, "dataProvider1", schema1Id, emptyList(), true, "query1"))
        val dataProvider2Id = dataProvider2.id!!
        val dataProvider3 = dataProviderRepository.save(DataProviderSQL(null, "dataProvider1", schema1Id, emptyList(), true, "query1"))
        val dataProvider3Id = dataProvider3.id!!

        //
        // Create some dataSources
        //

        val dataSource1 = dataSourceRepository.save(DataSource(null, "dataSource1", dataProvider1Id, setOf(1, 2, 3), setOf(1, 2), setOf(1)))
        val dataSource1Id = dataSource1.id!!

        val dataSources = dataSourceRepository.saveAll(listOf(
            DataSource(null, "dataSource2", dataProvider1Id, setOf(1, 2, 3), setOf(1, 2), setOf(1)),
            DataSource(null, "dataSource3", dataProvider2Id, setOf(1, 2, 3, 4), setOf(1), setOf(1)),
            DataSource(null, "dataSource4", dataProvider3Id, setOf(1, 2, 3), setOf(1, 2), setOf(1))))


        //
        // Test queries on filled repository
        //

        Assertions.assertEquals(4, dataSourceRepository.count())

        Assertions.assertEquals(4, dataSourceRepository.findAll().toList().size)
        Assertions.assertEquals(1, dataSourceRepository.findAllById(listOf(dataSource1Id)).toList().size)
        Assertions.assertEquals(2, dataSourceRepository.findAllByDataProviderId(dataProvider1Id).size)
        Assertions.assertEquals(3, dataSourceRepository.findAllByDataProviderIdIn(listOf(dataProvider1Id, dataProvider2Id)).size)

        Assertions.assertTrue(dataSourceRepository.existsById(dataSource1Id))

        Assertions.assertTrue(dataSourceRepository.findById(dataSource1Id).isPresent)
        Assertions.assertEquals("dataSource1", dataSource1.name)
        Assertions.assertEquals(dataProvider1Id, dataSource1.dataProviderId)
        Assertions.assertEquals(3, dataSource1.userAllowedToReadIds.size)
        Assertions.assertEquals(2, dataSource1.userAllowedToWriteIds.size)
        Assertions.assertEquals(1, dataSource1.userAllowedToDeleteIds.size)
        Assertions.assertTrue(dataSource1.userAllowedToReadIds.contains(1))
        Assertions.assertTrue(dataSource1.userAllowedToReadIds.contains(2))
        Assertions.assertTrue(dataSource1.userAllowedToReadIds.contains(3))
        Assertions.assertTrue(dataSource1.userAllowedToWriteIds.contains(1))
        Assertions.assertTrue(dataSource1.userAllowedToWriteIds.contains(2))
        Assertions.assertTrue(dataSource1.userAllowedToDeleteIds.contains(1))

        val dataSource3 = dataSources.first { it.name == "dataSource3" }

        Assertions.assertEquals("dataSource3", dataSource3.name)
        Assertions.assertEquals(dataProvider2Id, dataSource3.dataProviderId)
        Assertions.assertEquals(4, dataSource3.userAllowedToReadIds.size)
        Assertions.assertEquals(1, dataSource3.userAllowedToWriteIds.size)
        Assertions.assertEquals(1, dataSource3.userAllowedToDeleteIds.size)
        Assertions.assertTrue(dataSource3.userAllowedToReadIds.contains(1))
        Assertions.assertTrue(dataSource3.userAllowedToReadIds.contains(2))
        Assertions.assertTrue(dataSource3.userAllowedToReadIds.contains(3))
        Assertions.assertTrue(dataSource3.userAllowedToReadIds.contains(4))
        Assertions.assertTrue(dataSource3.userAllowedToWriteIds.contains(1))
        Assertions.assertTrue(dataSource3.userAllowedToDeleteIds.contains(1))

        //
        // Update existing dataSource
        //

        val dataSource1Updated = dataSourceRepository.save(DataSource(dataSource1Id, "dataSource5", dataProvider3Id, emptySet(), emptySet(), setOf(1)))

        Assertions.assertEquals(4, dataSourceRepository.count())
        Assertions.assertTrue(dataSourceRepository.findById(dataSource1Id).isPresent)
        Assertions.assertEquals("dataSource5", dataSource1Updated.name)
        Assertions.assertEquals(dataProvider3Id, dataSource1Updated.dataProviderId)
        Assertions.assertEquals(1, dataSource1Updated.userAllowedToReadIds.size)
        Assertions.assertEquals(1, dataSource1Updated.userAllowedToWriteIds.size)
        Assertions.assertEquals(1, dataSource1Updated.userAllowedToDeleteIds.size)
        Assertions.assertTrue(dataSource1Updated.userAllowedToReadIds.contains(1))
        Assertions.assertTrue(dataSource1Updated.userAllowedToWriteIds.contains(1))
        Assertions.assertTrue(dataSource1Updated.userAllowedToDeleteIds.contains(1))


        //
        // Update non existing dataSource
        //

        val dataSource6Updated = dataSourceRepository.save(DataSource(1000000, "dataSource6", dataProvider2Id, setOf(5), emptySet(), emptySet()))
        val dataSource6Id = dataSource6Updated.id!!

        Assertions.assertEquals(5, dataSourceRepository.count())
        Assertions.assertTrue(dataSourceRepository.findById(dataSource6Id).isPresent)
        Assertions.assertEquals("dataSource6", dataSource6Updated.name)
        Assertions.assertEquals(dataProvider2Id, dataSource6Updated.dataProviderId)
        Assertions.assertEquals(1, dataSource6Updated.userAllowedToReadIds.size)
        Assertions.assertEquals(0, dataSource6Updated.userAllowedToWriteIds.size)
        Assertions.assertEquals(0, dataSource6Updated.userAllowedToDeleteIds.size)
        Assertions.assertTrue(dataSource6Updated.userAllowedToReadIds.contains(5))


        Assertions.assertEquals(4, dataSourceRepository.findAllByUserAllowedToReadId(1).size)

        val readableByFour = dataSourceRepository.findAllByUserAllowedToReadId(4)
        Assertions.assertEquals(1, readableByFour.size)
        Assertions.assertEquals(dataSource3.id, readableByFour[0].id)

        val readableByFive = dataSourceRepository.findAllByUserAllowedToReadId(5)
        Assertions.assertEquals(1, readableByFive.size)
        Assertions.assertEquals(dataSource6Id, readableByFive[0].id)

        // Delete single dataSource
        dataSourceRepository.deleteById(dataSource1Id)
        Assertions.assertEquals(4, dataSourceRepository.count())

        dataSourceRepository.delete(dataSource6Updated)
        Assertions.assertEquals(3, dataSourceRepository.count())

        // Delete multiple dataSources
        dataSourceRepository.deleteAll(listOf(dataSources.first()))
        Assertions.assertEquals(2, dataSourceRepository.count())

        dataSourceRepository.deleteAll()
        Assertions.assertEquals(0, dataSourceRepository.count())
    }
}