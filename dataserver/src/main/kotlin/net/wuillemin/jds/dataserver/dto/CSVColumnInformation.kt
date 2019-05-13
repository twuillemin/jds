package net.wuillemin.jds.dataserver.dto

import net.wuillemin.jds.dataserver.entity.model.DataType

/**
 * The internal class used for gathering information about the column
 *
 * @param name the name of the column
 * @param dataType The best possible datatype for the column
 * @param nullable if the data contains null value
 * @param longestString The size of the longest data string
 */
data class CSVColumnInformation(
    val name: String,
    val dataType: DataType,
    val nullable: Boolean,
    val longestString: Int
)