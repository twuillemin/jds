package net.wuillemin.jds.dataserver.service.importation

import net.wuillemin.jds.dataserver.dto.CSVColumnInformation
import net.wuillemin.jds.dataserver.entity.model.DataType
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.springframework.stereotype.Service
import java.io.Reader
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime


/**
 * Service for guessing the model from a CSV file. The model can then be used
 * for example to create a table before importing the data of the CSV
 */
@Service
class CSVModelReader {

    /**
     * Read a CSV file an try to guess the type of the column. The first record is supposed to be the header
     *
     * @param input a reader on the data
     * @return a list of columns
     */
    fun guessCSVModel(input: Reader): List<CSVColumnInformation> {

        val records = CSVFormat.EXCEL
            .withFirstRecordAsHeader()
            .parse(input)

        var detectionStatus = records.headerMap.entries.map { ColumnDetectedInformation(name = it.key) }

        records.forEach { record ->
            detectionStatus = detectionStatus.refine(record)
        }

        return detectionStatus.map { (name, possibleTypes, nullable, longestString) ->
            CSVColumnInformation(
                name,
                finalizeDataType(possibleTypes),
                nullable,
                longestString)
        }
    }

    /**
     * Find the most common data type from a set of data type. By defect, a String is returned
     *
     * @param dataTypes The possible data types
     * @return The most common data type
     */
    private fun finalizeDataType(dataTypes: Set<DataType>): DataType {

        return when (dataTypes.size) {
            0    -> DataType.STRING
            1    -> dataTypes.first()
            else ->
                when {
                    // Prefer long over double if possible
                    dataTypes.contains(DataType.LONG)      -> DataType.LONG
                    dataTypes.contains(DataType.DOUBLE)    -> DataType.DOUBLE
                    // Boolean are alone
                    dataTypes.contains(DataType.BOOLEAN)   -> DataType.BOOLEAN
                    // Prefer date or time over date time if possible
                    dataTypes.contains(DataType.DATE)      -> DataType.DATE
                    dataTypes.contains(DataType.TIME)      -> DataType.TIME
                    dataTypes.contains(DataType.DATE_TIME) -> DataType.DATE_TIME
                    // Otherwise, string is good
                    else                                   -> DataType.STRING
                }
        }
    }

    /**
     * Refine a a list of ColumnDetectedInformation given a line read from the CSV: for each column, the list of possible types
     * or the nullable can be updated
     *
     * @param record The line read from the CSV
     * @return a new List of ColumnDetectedInformation
     */
    private fun List<ColumnDetectedInformation>.refine(record: CSVRecord): List<ColumnDetectedInformation> {

        return this.mapIndexed { index, existingInformation ->
            existingInformation.refine(record[index])
        }
    }

    /**
     * Refine a ColumnDetectedInformation given a value read from the CSV: update the list of possible types
     * or define as nullable
     *
     * @param csvValue The value to update the ColumnDetectedInformation
     * @return a new ColumnDetectedInformation
     */
    private fun ColumnDetectedInformation.refine(csvValue: String?): ColumnDetectedInformation {
        return csvValue
            ?.let {
                val value = it.trim()
                if (value.isNotEmpty()) {
                    this.copy(
                        possibleTypes = this.possibleTypes
                            .filter { dataType ->
                                when (dataType) {
                                    DataType.STRING          -> true
                                    DataType.LONG            -> value.toLongOrNull() != null
                                    DataType.DOUBLE          -> value.toDoubleOrNull() != null
                                    DataType.BOOLEAN         -> value == "0" || value == "1" || value.toLowerCase() == "true" || value.toLowerCase() == "false"
                                    DataType.DATE            -> try {
                                        LocalDate.parse(value)
                                        true
                                    }
                                    catch (e: Exception) {
                                        false
                                    }
                                    DataType.TIME            -> try {
                                        LocalTime.parse(value)
                                        true
                                    }
                                    catch (e: Exception) {
                                        false
                                    }
                                    DataType.DATE_TIME       -> try {
                                        OffsetDateTime.parse(value)
                                        true
                                    }
                                    catch (e: Exception) {
                                        false
                                    }
                                    // Can not import for now in list of strings as they are used only by lookup
                                    DataType.LIST_OF_STRINGS -> false
                                }
                            }
                            .toSet(),
                        longestString = maxOf(value.length, longestString))
                }
                else {
                    this.copy(nullable = true)
                }
            }
            ?: run {
                this.copy(nullable = true)
            }
    }

    /**
     * The object for storing the information detected on the column
     *
     * @param name the name of the column
     * @param possibleTypes The possible types of the column
     * @param nullable if the data contains null value
     * @param longestString The size of the longest data string
     */
    private data class ColumnDetectedInformation(
        val name: String,
        val possibleTypes: Set<DataType> = DataType.values().toSet(),
        val nullable: Boolean = false,
        val longestString: Int = 0
    )
}