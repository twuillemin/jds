package net.wuillemin.jds.dataserver.repository

import net.wuillemin.jds.dataserver.config.DataServerConfigDataBaseTest
import net.wuillemin.jds.dataserver.entity.model.ColumnAttribute
import net.wuillemin.jds.dataserver.entity.model.ColumnLookup
import net.wuillemin.jds.dataserver.entity.model.DataProviderGSheet
import net.wuillemin.jds.dataserver.entity.model.DataProviderSQL
import net.wuillemin.jds.dataserver.entity.model.DataType
import net.wuillemin.jds.dataserver.entity.model.ReadOnlyStorage
import net.wuillemin.jds.dataserver.entity.model.SchemaSQL
import net.wuillemin.jds.dataserver.entity.model.ServerSQL
import net.wuillemin.jds.dataserver.entity.model.WritableStorage
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [
    DataServerConfigDataBaseTest::class])
class DataProviderRepositoryTest {

    @Autowired
    private lateinit var serverRepository: ServerRepository
    @Autowired
    private lateinit var schemaRepository: SchemaRepository
    @Autowired
    private lateinit var dataProviderRepository: DataProviderRepository

    @Test
    fun `Can do basic operations`() {

        dataProviderRepository.deleteAll()

        //
        // Test queries on empty repository
        //

        Assertions.assertEquals(0, dataProviderRepository.count())

        Assertions.assertEquals(0, dataProviderRepository.findAll().toList().size)
        Assertions.assertEquals(0, dataProviderRepository.findAllById(listOf(1, 2)).size)
        Assertions.assertEquals(0, dataProviderRepository.findAllBySchemaId(1).size)
        Assertions.assertEquals(0, dataProviderRepository.findAllBySchemaIdIn(listOf(1, 2)).size)

        Assertions.assertFalse(dataProviderRepository.existsById(1))

        Assertions.assertFalse(dataProviderRepository.findById(1).isPresent)

        //
        // Create some server and schemas
        //

        val server1 = serverRepository.save(ServerSQL(null, "Server1", 1, true, "url", "user", "password", "driver"))
        val server1Id = server1.id!!
        val schema1 = schemaRepository.save(SchemaSQL(null, "schema1", server1Id, "role1"))
        val schema1Id = schema1.id!!
        val schema2 = schemaRepository.save(SchemaSQL(null, "schema2", server1Id, "role1"))
        val schema2Id = schema2.id!!
        val schema3 = schemaRepository.save(SchemaSQL(null, "schema3", server1Id, "role1"))
        val schema3Id = schema3.id!!

        val storageAttribute1 = ReadOnlyStorage(
            "readAttributeName1",
            true,
            false,
            true
        )

        val storageAttribute2 = WritableStorage(
            "containerName2",
            "readAttributeName2",
            "writeAttributeName2",
            false,
            true,
            false
        )

        val column1 = ColumnAttribute(
            "column1",
            DataType.STRING,
            25,
            storageAttribute1)

        val column2 = ColumnLookup(
            "column2",
            DataType.BOOLEAN,
            4,
            storageAttribute2,
            5,
            6,
            "keyColumnName",
            "valueColumnName")

        //
        // Create some dataProviders
        //

        val dataProvider1 = dataProviderRepository.save(DataProviderSQL(null, "dataProvider1", schema1Id, listOf(column1, column2), true, "query1"))
        val dataProvider1Id = dataProvider1.id!!

        val dataProviders = dataProviderRepository.saveAll(listOf(
            DataProviderSQL(null, "dataProvider2", schema1Id, listOf(column2), false, "query2"),
            DataProviderGSheet(null, "dataProvider3", schema2Id, listOf(column2, column1), false, "gsheet3"),
            DataProviderGSheet(null, "dataProvider4", schema3Id, listOf(column1), true, "gsheet4")))


        //
        // Test queries on filled repository
        //

        Assertions.assertEquals(4, dataProviderRepository.count())

        Assertions.assertEquals(4, dataProviderRepository.findAll().toList().size)
        Assertions.assertEquals(1, dataProviderRepository.findAllById(listOf(dataProvider1Id)).toList().size)
        Assertions.assertEquals(2, dataProviderRepository.findAllBySchemaId(schema1Id).size)
        Assertions.assertEquals(3, dataProviderRepository.findAllBySchemaIdIn(listOf(schema1Id, schema2Id)).size)

        Assertions.assertTrue(dataProviderRepository.existsById(dataProvider1Id))

        Assertions.assertTrue(dataProviderRepository.findById(dataProvider1Id).isPresent)
        Assertions.assertEquals("dataProvider1", dataProvider1.name)
        Assertions.assertEquals(schema1Id, dataProvider1.schemaId)
        Assertions.assertEquals(2, dataProvider1.columns.size)
        Assertions.assertEquals(column1, dataProvider1.columns[0])
        Assertions.assertEquals(column2, dataProvider1.columns[1])
        Assertions.assertTrue(dataProvider1.editable)
        Assertions.assertEquals("query1", dataProvider1.query)

        val dataProvider3 = dataProviders.first { it.name == "dataProvider3" }

        Assertions.assertEquals("dataProvider3", dataProvider3.name)
        Assertions.assertEquals(schema2Id, dataProvider3.schemaId)
        Assertions.assertEquals(2, dataProvider3.columns.size)
        Assertions.assertEquals(column2, dataProvider3.columns[0])
        Assertions.assertEquals(column1, dataProvider3.columns[1])
        Assertions.assertFalse(dataProvider3.editable)

        when (dataProvider3) {
            is DataProviderGSheet -> {
                Assertions.assertEquals("gsheet3", dataProvider3.sheetName)
            }
            else                  -> Assertions.fail()
        }

        //
        // Update existing dataProvider sql -> gsheet
        //

        val dataProvider1Updated = dataProviderRepository.save(DataProviderGSheet(dataProvider1Id, "dataProvider5", schema3Id, listOf(column2), false, "gsheet5"))

        Assertions.assertEquals(4, dataProviderRepository.count())
        Assertions.assertTrue(dataProviderRepository.findById(dataProvider1Id).isPresent)
        Assertions.assertEquals("dataProvider5", dataProvider1Updated.name)
        Assertions.assertEquals(schema3Id, dataProvider1Updated.schemaId)
        Assertions.assertEquals(1, dataProvider1Updated.columns.size)
        Assertions.assertEquals(column2, dataProvider1Updated.columns[0])
        Assertions.assertFalse(dataProvider1Updated.editable)
        Assertions.assertEquals("gsheet5", dataProvider1Updated.sheetName)

        //
        // Update existing dataProvider gsheet -> sql
        //

        val dataProvider1Updated2 = dataProviderRepository.save(DataProviderSQL(dataProvider1Id, "dataProvider1", schema1Id, listOf(column2, column1, column1, column2), true, "query1"))

        Assertions.assertEquals(4, dataProviderRepository.count())
        Assertions.assertEquals("dataProvider1", dataProvider1Updated2.name)
        Assertions.assertEquals(schema1Id, dataProvider1Updated2.schemaId)
        Assertions.assertEquals(4, dataProvider1Updated2.columns.size)
        Assertions.assertEquals(column2, dataProvider1Updated2.columns[0])
        Assertions.assertEquals(column1, dataProvider1Updated2.columns[1])
        Assertions.assertEquals(column1, dataProvider1Updated2.columns[2])
        Assertions.assertEquals(column2, dataProvider1Updated2.columns[3])
        Assertions.assertTrue(dataProvider1Updated2.editable)
        Assertions.assertEquals("query1", dataProvider1Updated2.query)

        //
        // Update non existing dataProvider
        //

        val dataProvider6Updated = dataProviderRepository.save(DataProviderGSheet(1000000, "dataProvider6", schema2Id, listOf(column1), false, "gsheet6"))
        val dataProvider6Id = dataProvider6Updated.id!!

        Assertions.assertEquals(5, dataProviderRepository.count())
        Assertions.assertTrue(dataProviderRepository.findById(dataProvider6Id).isPresent)
        Assertions.assertEquals("dataProvider6", dataProvider6Updated.name)
        Assertions.assertEquals(schema2Id, dataProvider6Updated.schemaId)
        Assertions.assertEquals(1, dataProvider6Updated.columns.size)
        Assertions.assertEquals(column1, dataProvider6Updated.columns[0])
        Assertions.assertFalse(dataProvider6Updated.editable)
        Assertions.assertEquals("gsheet6", dataProvider6Updated.sheetName)

        // Delete single dataProvider
        dataProviderRepository.deleteById(dataProvider1Id)
        Assertions.assertEquals(4, dataProviderRepository.count())

        dataProviderRepository.delete(dataProvider6Updated)
        Assertions.assertEquals(3, dataProviderRepository.count())

        // Delete multiple dataProviders
        dataProviderRepository.deleteAll(listOf(dataProviders.first()))
        Assertions.assertEquals(2, dataProviderRepository.count())

        dataProviderRepository.deleteAll()
        Assertions.assertEquals(0, dataProviderRepository.count())
    }
}