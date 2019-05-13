package net.wuillemin.jds.dataserver.dto

/**
 * Represents a preview of Table / DataProviderSQL / DataSource from a connector
 *
 * @param columns The columns of data
 * @param data The data first rows
 */
data class Preview(
    val columns: List<TableColumnMeta>,
    val data: List<Map<String, Any>>
)