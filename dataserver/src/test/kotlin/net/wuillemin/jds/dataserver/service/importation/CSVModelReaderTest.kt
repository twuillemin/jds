package net.wuillemin.jds.dataserver.service.importation

import net.wuillemin.jds.dataserver.entity.model.DataType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CSVModelReaderTest {

    private val reader = CSVModelReader()


    @Test
    fun `Read Simple File`() {

        val csvData = "string,long,double,boolean,date,time,datetime,stringNull,longNull,doubleNull,booleanNull,dateNull,timeNull,datetimeNull\n" +
            "abc   ,5 ,5  ,true ,2018-05-10,10:00:01,2018-10-21T10:00:01Z,abc def,5,5.2,true,2018-05-10,10:00:01,2018-10-21T10:00:01Z\n" +
            "abcdef,5 ,5.2,false,2018-05-11,10:00:02,2018-10-21T10:00:01Z,,,,,,,\n" +
            "abc   ,6 ,5.2,true ,2018-05-12,10:00:03,2018-10-21T10:00:01Z,abc def,5,5.2,true,2018-05-10,10:00:01,2018-10-21T10:00:01Z\n"

        val columns = reader.guessCSVModel(csvData.reader())

        assertEquals(DataType.STRING, columns[0].dataType)
        assertEquals(6, columns[0].longestString)
        assertFalse(columns[0].nullable)

        assertEquals(DataType.LONG, columns[1].dataType)
        assertEquals(1, columns[1].longestString)
        assertFalse(columns[1].nullable)

        assertEquals(DataType.DOUBLE, columns[2].dataType)
        assertEquals(3, columns[2].longestString)
        assertFalse(columns[2].nullable)

        assertEquals(DataType.BOOLEAN, columns[3].dataType)
        assertEquals(5, columns[3].longestString)
        assertFalse(columns[3].nullable)

        assertEquals(DataType.DATE, columns[4].dataType)
        assertEquals(10, columns[4].longestString)
        assertFalse(columns[4].nullable)

        assertEquals(DataType.TIME, columns[5].dataType)
        assertEquals(8, columns[5].longestString)
        assertFalse(columns[5].nullable)

        assertEquals(DataType.DATE_TIME, columns[6].dataType)
        assertEquals(20, columns[6].longestString)
        assertFalse(columns[6].nullable)

        assertEquals(DataType.STRING, columns[7].dataType)
        assertEquals(7, columns[7].longestString)
        assertTrue(columns[7].nullable)

        assertEquals(DataType.LONG, columns[8].dataType)
        assertEquals(1, columns[8].longestString)
        assertTrue(columns[8].nullable)

        assertEquals(DataType.DOUBLE, columns[9].dataType)
        assertEquals(3, columns[9].longestString)
        assertTrue(columns[9].nullable)

        assertEquals(DataType.BOOLEAN, columns[10].dataType)
        assertEquals(4, columns[10].longestString)
        assertTrue(columns[10].nullable)

        assertEquals(DataType.DATE, columns[11].dataType)
        assertEquals(10, columns[11].longestString)
        assertTrue(columns[11].nullable)

        assertEquals(DataType.TIME, columns[12].dataType)
        assertEquals(8, columns[12].longestString)
        assertTrue(columns[12].nullable)

        assertEquals(DataType.DATE_TIME, columns[13].dataType)
        assertEquals(20, columns[13].longestString)
        assertTrue(columns[13].nullable)
    }
}