package net.wuillemin.jds.dataserver.supplier.sql.util

import net.wuillemin.jds.common.exception.ConstraintException
import net.wuillemin.jds.dataserver.entity.model.DataType
import net.wuillemin.jds.dataserver.exception.E
import org.springframework.stereotype.Service
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types.BIGINT
import java.sql.Types.BIT
import java.sql.Types.BOOLEAN
import java.sql.Types.CHAR
import java.sql.Types.CLOB
import java.sql.Types.DATE
import java.sql.Types.DECIMAL
import java.sql.Types.DOUBLE
import java.sql.Types.FLOAT
import java.sql.Types.INTEGER
import java.sql.Types.LONGNVARCHAR
import java.sql.Types.LONGVARCHAR
import java.sql.Types.NCHAR
import java.sql.Types.NCLOB
import java.sql.Types.NUMERIC
import java.sql.Types.NVARCHAR
import java.sql.Types.REAL
import java.sql.Types.ROWID
import java.sql.Types.SMALLINT
import java.sql.Types.TIME
import java.sql.Types.TIMESTAMP
import java.sql.Types.TIMESTAMP_WITH_TIMEZONE
import java.sql.Types.TIME_WITH_TIMEZONE
import java.sql.Types.TINYINT
import java.sql.Types.VARCHAR
import java.time.Instant
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneId


/**
 * Helper functions for working with JDBC
 */
@Service
class JDBCHelper {

    /**
     * Convert a JDBC type to an internal data type
     *
     * @param jdbcType The type to convert
     * @return The corresponding internal data type
     * @throws ConstraintException if the column type is not supported
     */
    fun getDataTypeFromJdbcType(jdbcType: Int): DataType {

        when (jdbcType) {
            CHAR, CLOB, LONGNVARCHAR, LONGVARCHAR, NCHAR, NCLOB, NVARCHAR, VARCHAR -> return DataType.STRING

            BIGINT, BIT, INTEGER, ROWID, SMALLINT, TINYINT                         -> return DataType.LONG

            DECIMAL, DOUBLE, FLOAT, NUMERIC, REAL                                  -> return DataType.DOUBLE

            BOOLEAN                                                                -> return DataType.BOOLEAN

            DATE                                                                   -> return DataType.DATE

            TIME, TIME_WITH_TIMEZONE                                               -> return DataType.TIME

            TIMESTAMP, TIMESTAMP_WITH_TIMEZONE                                     -> return DataType.DATE_TIME
        }

        throw ConstraintException(E.supplier.sql.util.jdbcTypeNotSupported, jdbcType)
    }

    /**
     * Convert a full ResultSet coming from an SQL query to a dictionary of value. Values
     * returned are directly compatible with internal objects. For example, the SQL type date
     * is returned as a LocalDate. Non named columns of the ResultSet are skipped.
     *
     * @param resultSet  The ResultSet to be converted
     * @param defaultTimeZone A default timeZone for the conversion of Date/Time.
     * @return A dictionary of converted values. If some of the ResultSet values are null,their relative
     * entries won't be present in the dictionary
     * @throws SQLException if a database access error occurs or if one of the columns types is not supported
     */
    @Throws(SQLException::class)
    fun convertResultSet(
        resultSet: ResultSet,
        defaultTimeZone: ZoneId): Map<String, Any> {

        // JDBC columns start at 1 and range are inclusive
        return IntRange(1, resultSet.metaData.columnCount)
            .mapNotNull { index ->
                resultSet.metaData.getColumnName(index)?.let { Pair(index, it) }
            }
            .mapNotNull { (index, columnName) ->
                convertColumn(resultSet, index, defaultTimeZone)?.let { Pair(columnName, it) }
            }
            .toMap()
    }

    /**
     * Convert a single column of a  ResultSet to a dictionary of value.
     *
     * @param resultSet   The ResultSet to be converted
     * @param columnIndex The index of the column to be read in the ResultSet
     * @param defaultTimeZone  A default timeZone for the conversion of Date/Time.
     * @return A single value. If the column JDBC type is not supported, then an SQLException is thrown
     * @throws SQLException if a database access error occurs or if the column type is not supported
     */
    @Throws(SQLException::class)
    private fun convertColumn(
        resultSet: ResultSet,
        columnIndex: Int,
        defaultTimeZone: ZoneId): Any? {

        // ------------------------------------------------------------
        //
        // DO NOT CHANGE THIS CLASS WITHOUT CHANGING ALSO JdbcHelper
        //
        // ------------------------------------------------------------

        val possibleResult: Any? = when (resultSet.metaData.getColumnType(columnIndex)) {
            CHAR, CLOB, LONGNVARCHAR, LONGVARCHAR, NCHAR, NCLOB, NVARCHAR, VARCHAR -> resultSet.getString(columnIndex)

            BIGINT, BIT, INTEGER, ROWID, SMALLINT, TINYINT                         -> resultSet.getLong(columnIndex)

            DECIMAL, DOUBLE, FLOAT, NUMERIC, REAL                                  -> resultSet.getDouble(columnIndex)

            BOOLEAN                                                                -> resultSet.getBoolean(columnIndex)

            DATE                                                                   -> resultSet.getDate(columnIndex)?.toLocalDate()

            TIME                                                                   -> resultSet.getTime(columnIndex)?.toLocalTime()

            // Warning: this mapping is not defined by JDBC. So the object returned is at the mercy of the JDBC driver
            // SQL server seems to return an OffsetTime
            TIME_WITH_TIMEZONE                                                     -> resultSet.getObject(columnIndex, OffsetTime::class.java)

            TIMESTAMP                                                              -> resultSet.getTimestamp(columnIndex)?.let { OffsetDateTime.ofInstant(Instant.ofEpochMilli(it.time), defaultTimeZone) }

            // Warning: this mapping is not defined by JDBC. So the object returned is at the mercy of the JDBC driver
            // SQL server seems to return an OffsetDateTime
            TIMESTAMP_WITH_TIMEZONE                                                -> resultSet.getObject(columnIndex, OffsetDateTime::class.java)

            else                                                                   -> {
                throw ConstraintException(E.supplier.sql.util.jdbcColumnTypeNotSupported, columnIndex, resultSet.metaData.getColumnType(columnIndex))
            }
        }

        // Return only if there is something coherent
        return if (possibleResult != null && !resultSet.wasNull()) {
            possibleResult
        }
        else {
            null
        }
    }
}