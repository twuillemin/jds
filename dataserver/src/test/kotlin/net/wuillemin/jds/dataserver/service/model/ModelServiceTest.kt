package net.wuillemin.jds.dataserver.service.model

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.dataserver.entity.model.DataProviderSQL
import net.wuillemin.jds.dataserver.entity.model.SchemaGSheet
import net.wuillemin.jds.dataserver.entity.model.SchemaSQL
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLModelReader
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLModelWriter
import net.wuillemin.jds.dataserver.supplier.sql.service.SQLQueryImporter
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.test.context.junit.jupiter.SpringExtension


@ExtendWith(SpringExtension::class)
class ModelServiceTest {

    private var sqlQueryImporter: SQLQueryImporter = mock(SQLQueryImporter::class.java)
    private var dataProviderService: DataProviderService = mock(DataProviderService::class.java)

    @BeforeEach
    fun beforeEach() {
        sqlQueryImporter = mock(SQLQueryImporter::class.java)
        dataProviderService = mock(DataProviderService::class.java)
    }

    @Test
    fun `Create DataProvider From SQL calls services`() {

        val sqlModelReader = mock(SQLModelReader::class.java)
        val sqlModelWriter = mock(SQLModelWriter::class.java)

        val modelImporter = ModelService(sqlModelReader, sqlModelWriter, sqlQueryImporter, dataProviderService)

        val schema = SchemaSQL(
            id = "schemaId25",
            name = "PUBLIC",
            serverId = "serverId",
            roleName = null)

        val dataProvider = DataProviderSQL(
            id = null,
            schemaId = "serverId25",
            name = "The name to create",
            columns = emptyList(),
            editable = false,
            query = "select * from table")

        whenever(sqlQueryImporter.buildDataProviderFromQuery(eq(schema), eq("The name to create"), eq("select * from table"))).thenReturn(dataProvider)
        whenever(dataProviderService.addDataProvider(eq(dataProvider))).thenReturn(dataProvider)

        val dataProviderResult = modelImporter.createDataProviderFromSQL(
            schema,
            "The name to create",
            "select * from table")

        assertNotNull(dataProviderResult)
        verify(sqlQueryImporter, Mockito.times(1)).buildDataProviderFromQuery(any(), any(), any())
        verify(dataProviderService, Mockito.times(1)).addDataProvider(any())
    }

    @Test
    fun `Create DataProvider From SQL checks type of server`() {

        val sqlModelReader = mock(SQLModelReader::class.java)
        val sqlModelWriter = mock(SQLModelWriter::class.java)

        val modelImporter = ModelService(sqlModelReader, sqlModelWriter, sqlQueryImporter, dataProviderService)

        assertThrows(BadParameterException::class.java)
        {
            modelImporter.createDataProviderFromSQL(
                SchemaGSheet(
                    id = null,
                    name = "name",
                    serverId = "serverGSheetId"),
                "name",
                "select * from table")
        }
    }
}