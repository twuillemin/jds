package net.wuillemin.jds.dataserver.repository

import net.wuillemin.jds.dataserver.entity.model.Column
import net.wuillemin.jds.dataserver.entity.model.ColumnAttribute
import net.wuillemin.jds.dataserver.entity.model.ColumnLookup
import net.wuillemin.jds.dataserver.entity.model.DataProvider
import net.wuillemin.jds.dataserver.entity.model.DataProviderGSheet
import net.wuillemin.jds.dataserver.entity.model.DataProviderSQL
import net.wuillemin.jds.dataserver.entity.model.DataType
import net.wuillemin.jds.dataserver.entity.model.ReadOnlyStorage
import net.wuillemin.jds.dataserver.entity.model.Schema
import net.wuillemin.jds.dataserver.entity.model.WritableStorage
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.repository.CrudRepository
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.*

// Definition of constants
private const val TYPE_DATAPROVIDER_SQL = "sql"
private const val TYPE_DATAPROVIDER_GSHEET = "gsheet"
private const val TYPE_COLUMN_ATTRIBUTE = "attribute"
private const val TYPE_COLUMN_LOOKUP = "lookup"
private const val TYPE_STORAGE_READ_ONLY = "read_only"
private const val TYPE_STORAGE_WRITABLE = "writable"

/**
 * The repository for [DataProvider] objects
 */
@Suppress("SqlResolve")
@Repository
class DataProviderRepository(
    @Qualifier("dataserverJdbcTemplate") private val jdbcTemplate: JdbcTemplate,
    private val dataSourceRepository: DataSourceRepository) : CrudRepository<DataProvider, Long> {

    private val dataProviderSelectColumns = "id, type, name, schema_id, editable, sql_query, gsheet_sheet_name"
    private val dataProviderRowMapper = DataProviderRowMapper()

    private val columnSelectColumns = "type, name, data_type, size, lookup_maximum_number, lookup_data_source_id, lookup_key_column, lookup_value_column, storage_type, storage_nullable, storage_primary_key, storage_auto_increment, storage_read_attr_name, storage_write_attr_name, storage_container_name"
    private val columnRowMapper = ColumnRowMapper()

    private val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate.dataSource!!)

    /**
     * Returns the number of dataProviders available.
     *
     * @return the number of dataProviders
     */
    override fun count(): Long {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM jds_dataprovider", Long::class.java) ?: 0
    }

    /**
     * Returns all dataProviders.
     *
     * @return all dataProviders
     */
    override fun findAll(): Iterable<DataProvider> {
        return jdbcTemplate.query("SELECT $dataProviderSelectColumns FROM jds_dataprovider", dataProviderRowMapper)
    }

    /**
     * Returns all instances of the dataProviders with the given IDs.
     *
     * @param ids The ids
     * @return the dataProviders
     */
    override fun findAllById(ids: Iterable<Long>): Iterable<DataProvider> {
        return namedTemplate.query(
            "SELECT $dataProviderSelectColumns FROM jds_dataprovider WHERE id IN (:ids)",
            MapSqlParameterSource("ids", ids),
            dataProviderRowMapper)
    }

    /**
     * Return the list of dataProviders referencing the given [Schema] id
     *
     * @param schemaId The id of the schema referenced
     * @return the list of data providers referencing the given schema id
     */
    fun findBySchemaId(schemaId: Long): List<DataProvider> {
        return jdbcTemplate.query(
            "SELECT $dataProviderSelectColumns FROM jds_dataprovider WHERE schema_id = ?",
            arrayOf<Any>(schemaId),
            dataProviderRowMapper)
    }

    /**
     * Return the list of dataProviders referencing the given [Schema] id
     *
     * @param schemaIds The id of the schemas referenced
     * @return the list of data providers referencing the given schema ids
     */
    fun findBySchemaIdIn(schemaIds: List<Long>): List<DataProvider> {
        return namedTemplate.query(
            "SELECT $dataProviderSelectColumns FROM jds_dataprovider WHERE schema_id IN (:schemaIds)",
            MapSqlParameterSource("schemaIds", schemaIds),
            dataProviderRowMapper)
    }

    /**
     * Returns whether an dataProvider with the given id exists.
     *
     * @param id The id
     * @return {@literal true} if an dataProvider with the given id exists, {@literal false} otherwise.
     */
    override fun existsById(id: Long): Boolean {
        return jdbcTemplate
            .queryForObject(
                "SELECT COUNT(*) FROM jds_dataprovider WHERE id = ?",
                arrayOf<Any>(id),
                Long::class.java)
            .let { it > 0 }
    }

    /**
     * Retrieves an dataProvider by its id.
     *
     * @param id The id of the dataProvider to retrieve.
     * @return the dataProvider with the given id or {@literal Optional#empty()} if none found
     */
    override fun findById(id: Long): Optional<DataProvider> {
        return Optional.ofNullable(
            jdbcTemplate.queryForObject(
                "SELECT $dataProviderSelectColumns FROM jds_dataprovider WHERE id = ?",
                arrayOf<Any>(id),
                dataProviderRowMapper))
    }

    /**
     * Saves a given dataProvider. Use the returned dataProvider for further operations as the save operation might have changed the
     * entity instance completely.
     *
     * @param dataProvider The dataProvider to save
     * @return the saved dataProvider
     */
    override fun <S : DataProvider> save(dataProvider: S): S {

        return findById(upsert(dataProvider)).get() as S
    }

    /**
     * Saves all given dataProviders.
     *
     * @param dataProviders The dataProviders to save
     * @return the saved dataProviders.
     */
    override fun <S : DataProvider> saveAll(dataProviders: Iterable<S>): Iterable<S> {

        // Upsert all the dataProviders
        val ids = dataProviders.map { upsert(it) }
        // Reload them
        return findAllById(ids) as Iterable<S>
    }

    /**
     * Deletes all entities managed by the repository.
     */
    override fun deleteAll() {
        dataSourceRepository.deleteAll()
        jdbcTemplate.execute("TRUNCATE TABLE jds_dataprovider_column")
        jdbcTemplate.execute("DELETE FROM jds_dataprovider")
    }

    /**
     * Deletes the given dataProviders.
     *
     * @param dataProviders The dataProviders to delete
     */
    override fun deleteAll(dataProviders: Iterable<DataProvider>) {

        // Delete the columns
        namedTemplate.update(
            "DELETE FROM jds_dataprovider_column WHERE dataprovider_id IN (:ids)",
            MapSqlParameterSource("ids", dataProviders.mapNotNull { it.id }))

        namedTemplate.update(
            "DELETE FROM jds_dataprovider WHERE id IN (:ids)",
            MapSqlParameterSource("ids", dataProviders.mapNotNull { it.id }))
    }

    /**
     * Deletes a given dataProvider.
     *
     * @param dataProvider The dataProvider to delete
     */
    override fun delete(dataProvider: DataProvider) {
        dataProvider.id
            ?.let { dataProviderId ->

                jdbcTemplate.update(
                    "DELETE FROM jds_dataprovider_column WHERE dataprovider_id = ?",
                    arrayOf<Any>(dataProviderId))

                jdbcTemplate.update(
                    "DELETE FROM jds_dataprovider WHERE id = ?",
                    arrayOf<Any>(dataProviderId))
            }
            ?: throw IllegalArgumentException("Unable to delete a non persisted DataProvider object")
    }

    /**
     * Deletes the dataProvider with the given id.
     *
     * @param id The id to delete
     */
    override fun deleteById(id: Long) {

        jdbcTemplate.update(
            "DELETE FROM jds_dataprovider_column WHERE dataprovider_id = ?",
            arrayOf<Any>(id))

        jdbcTemplate.update(
            "DELETE FROM jds_dataprovider WHERE id = ?",
            arrayOf<Any>(id))
    }

    /**
     * Save an dataProvider by creating it or updating it. If the given dataProvider has an id, but this id does not exist in the
     * database, a new entry is created in the database, possibly with another id.
     *
     * @param dataProvider The dataProvider to delete
     * @return the id of the saved entity
     */
    private fun upsert(dataProvider: DataProvider): Long {

        val dataProviderId = dataProvider.id
            ?.let { dataProviderId ->
                if (existsById(dataProviderId)) {
                    val args = when (dataProvider) {
                        is DataProviderSQL    -> arrayOf(
                            TYPE_DATAPROVIDER_SQL,
                            dataProvider.name,
                            dataProvider.schemaId,
                            dataProvider.editable,
                            dataProvider.query,
                            null,
                            dataProvider.id)
                        is DataProviderGSheet -> arrayOf(
                            TYPE_DATAPROVIDER_GSHEET,
                            dataProvider.name,
                            dataProvider.schemaId,
                            dataProvider.editable,
                            null,
                            dataProvider.sheetName,
                            dataProvider.id)
                    }

                    jdbcTemplate.update(
                        "UPDATE jds_dataprovider SET type = ?, name = ?, schema_id = ?, editable = ?, sql_query = ?, gsheet_sheet_name = ? WHERE id = ?",
                        args,
                        arrayOf(
                            java.sql.Types.VARCHAR,
                            java.sql.Types.BIGINT,
                            java.sql.Types.BOOLEAN,
                            java.sql.Types.VARCHAR,
                            java.sql.Types.VARCHAR,
                            java.sql.Types.BIGINT).toIntArray())
                    dataProviderId
                }
                else {
                    val keyHolder = GeneratedKeyHolder()
                    jdbcTemplate.update(mapInsert(dataProvider), keyHolder)
                    keyHolder.key as Long
                }
            }
            ?: run {
                val keyHolder = GeneratedKeyHolder()
                jdbcTemplate.update(mapInsert(dataProvider), keyHolder)
                keyHolder.key as Long
            }

        // Delete old columns
        jdbcTemplate.update(
            "DELETE FROM jds_dataprovider_column WHERE dataprovider_id = ?",
            arrayOf<Any>(dataProviderId))

        // Insert the columns
        this.jdbcTemplate.batchUpdate(
            "INSERT INTO jds_dataprovider_column(data_provider_id, column_index, type, name, data_type, size, lookup_maximum_number, lookup_data_source_id, lookup_key_column, lookup_value_column, storage_type, storage_nullable, storage_primary_key, storage_auto_increment, storage_read_attr_name, storage_write_attr_name, storage_container_name) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            object : BatchPreparedStatementSetter {

                @Throws(SQLException::class)
                override fun setValues(ps: PreparedStatement, i: Int) {

                    val column = dataProvider.columns[i]

                    ps.setLong(1, dataProviderId)
                    ps.setInt(2, i)
                    ps.setString(4, column.name)
                    ps.setString(5, column.dataType.toString())
                    ps.setInt(6, column.size)

                    when (column) {
                        is ColumnAttribute -> {
                            ps.setString(3, TYPE_COLUMN_ATTRIBUTE)
                            ps.setNull(7, java.sql.Types.INTEGER)
                            ps.setNull(8, java.sql.Types.BIGINT)
                            ps.setNull(9, java.sql.Types.VARCHAR)
                            ps.setNull(10, java.sql.Types.VARCHAR)
                        }
                        is ColumnLookup    -> {
                            ps.setString(3, TYPE_COLUMN_LOOKUP)
                            ps.setInt(7, column.maximumNumberOfLookups)
                            ps.setLong(8, column.dataSourceId)
                            ps.setString(9, column.keyColumnName)
                            ps.setString(10, column.valueColumnName)
                        }
                    }

                    val storageDetail = column.storageDetail

                    ps.setBoolean(12, storageDetail.nullable)
                    ps.setBoolean(13, storageDetail.primaryKey)
                    ps.setBoolean(14, storageDetail.autoIncrement)
                    ps.setString(15, storageDetail.readAttributeName)

                    when (storageDetail) {
                        is ReadOnlyStorage -> {
                            ps.setString(11, TYPE_STORAGE_READ_ONLY)
                            ps.setNull(16, java.sql.Types.VARCHAR)
                            ps.setNull(17, java.sql.Types.VARCHAR)
                        }
                        is WritableStorage -> {
                            ps.setString(11, TYPE_STORAGE_WRITABLE)
                            ps.setString(16, storageDetail.writeAttributeName)
                            ps.setString(17, storageDetail.containerName)
                        }
                    }
                }

                override fun getBatchSize(): Int {
                    return dataProvider.columns.size
                }
            })

        return dataProviderId
    }

    /**
     * Create the mapping function returning the [PreparedStatement] for inserting an [DataProvider]
     *
     * @param dataProvider The dataProvider to insert
     * @return a function creating a prepared statement from a connection
     */
    private fun mapInsert(dataProvider: DataProvider): ((Connection) -> PreparedStatement) {

        return { connection ->
            val ps = connection.prepareStatement("INSERT INTO jds_dataprovider(type, name, schema_id, editable, sql_query, gsheet_sheet_name) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)

            when (dataProvider) {
                is DataProviderSQL    -> {
                    ps.setString(1, TYPE_DATAPROVIDER_SQL)
                    ps.setString(2, dataProvider.name)
                    ps.setLong(3, dataProvider.schemaId)
                    ps.setBoolean(4, dataProvider.editable)
                    ps.setString(5, dataProvider.query)
                    ps.setNull(6, java.sql.Types.VARCHAR)
                }
                is DataProviderGSheet -> {
                    ps.setString(1, TYPE_DATAPROVIDER_SQL)
                    ps.setString(2, dataProvider.name)
                    ps.setLong(3, dataProvider.schemaId)
                    ps.setBoolean(4, dataProvider.editable)
                    ps.setNull(5, java.sql.Types.VARCHAR)
                    ps.setString(6, dataProvider.sheetName)
                }
            }
            ps
        }
    }

    /**
     * Class doing the mapping from a database entry to an [DataProvider] object
     */
    inner class DataProviderRowMapper : RowMapper<DataProvider> {

        /**
         * Map a single row of data given as a [ResultSet] to an [DataProvider]
         * @param rs the ResultSet to map (pre-initialized for the current row)
         * @param rowNum the number of the current row
         * @return the DataProvider object for the current row (may be {@code null})
         * @throws SQLException if a SQLException is encountered getting
         * column values (that is, there's no need to catch SQLException)
         */
        @Throws(SQLException::class)
        override fun mapRow(rs: ResultSet, rowNum: Int): DataProvider {

            val id = rs.getLong(1)
            val type = rs.getString(2)
            val name = rs.getString(3)
            val schemaId = rs.getLong(4)
            val enabled = rs.getBoolean(5)

            val columns = jdbcTemplate.query(
                "SELECT $columnSelectColumns FROM jds_dataprovider_column WHERE data_provider_id = ? ORDER BY column_index",
                arrayOf<Any>(id),
                columnRowMapper)

            return when (type) {
                TYPE_DATAPROVIDER_SQL    -> DataProviderSQL(
                    id,
                    name,
                    schemaId,
                    columns,
                    enabled,
                    rs.getString(6)
                )
                TYPE_DATAPROVIDER_GSHEET -> DataProviderGSheet(
                    id,
                    name,
                    schemaId,
                    columns,
                    enabled,
                    rs.getString(7)
                )
                else                     -> throw SQLException("Unknown DataProvider type when reading from database \"$type\"")
            }
        }
    }

    /**
     * Class doing the mapping from a database entry to an [Column] object
     */
    inner class ColumnRowMapper : RowMapper<Column> {

        /**
         * Map a single row of data given as a [ResultSet] to an [DataProvider]
         * @param rs the ResultSet to map (pre-initialized for the current row)
         * @param rowNum the number of the current row
         * @return the DataProvider object for the current row (may be {@code null})
         * @throws SQLException if a SQLException is encountered getting
         * column values (that is, there's no need to catch SQLException)
         */
        @Throws(SQLException::class)
        override fun mapRow(rs: ResultSet, rowNum: Int): Column {

            val type = rs.getString(1)
            val name = rs.getString(2)
            val dataType = DataType.valueOf(rs.getString(3))
            val size = rs.getInt(4)

            val storageType = rs.getString(9)
            val storageNullable = rs.getBoolean(10)
            val storagePrimaryKey = rs.getBoolean(11)
            val storageAutoIncrement = rs.getBoolean(12)
            val storageReadAttributeName = rs.getString(13)

            val storageDetail = when (storageType) {
                TYPE_STORAGE_READ_ONLY -> ReadOnlyStorage(
                    storageReadAttributeName,
                    storageNullable,
                    storagePrimaryKey,
                    storageAutoIncrement)
                TYPE_STORAGE_WRITABLE  -> {
                    val storageWriteAttributeName = rs.getString(14)
                    val storageContainerName = rs.getString(15)

                    WritableStorage(
                        storageContainerName,
                        storageReadAttributeName,
                        storageWriteAttributeName,
                        storageNullable,
                        storagePrimaryKey,
                        storageAutoIncrement)
                }
                else                   -> throw SQLException("Unknown Column storage_type when reading from database \"$type\"")
            }

            return when (type) {

                TYPE_COLUMN_ATTRIBUTE -> ColumnAttribute(
                    name,
                    dataType,
                    size,
                    storageDetail
                )
                TYPE_COLUMN_LOOKUP    -> {

                    val lookupMaximumNumber = rs.getInt(5)
                    val lookupDataSourceId = rs.getLong(6)
                    val lookupKeyColumnName = rs.getString(7)
                    val lookupValueColumnName = rs.getString(8)

                    ColumnLookup(
                        name,
                        dataType,
                        size,
                        storageDetail,
                        lookupMaximumNumber,
                        lookupDataSourceId,
                        lookupKeyColumnName,
                        lookupValueColumnName
                    )
                }
                else                  -> throw SQLException("Unknown Column type when reading from database \"$type\"")
            }
        }
    }
}
