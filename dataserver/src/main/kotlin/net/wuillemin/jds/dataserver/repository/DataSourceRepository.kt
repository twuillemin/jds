package net.wuillemin.jds.dataserver.repository

import net.wuillemin.jds.dataserver.entity.model.DataProvider
import net.wuillemin.jds.dataserver.entity.model.DataSource
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
private const val ALLOWED_READ = "R"
private const val ALLOWED_WRITE = "W"
private const val ALLOWED_DELETE = "D"

/**
 * The repository of the [DataSource]s that are stored in the database
 */
@Suppress("SqlResolve")
@Repository
class DataSourceRepository(@Qualifier("dataserverJdbcTemplate") private val jdbcTemplate: JdbcTemplate) : CrudRepository<DataSource, Long> {


    private val dataSourceSelectColumns = "id, name, data_provider_id"
    private val dataSourceRowMapper = DataSourceRowMapper()

    private val userRightSelectColumns = "user_id, permission"
    private val userRightRowMapper = UserRightRowMapper()

    private val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate.dataSource!!)

    /**
     * Returns the number of dataSources available.
     *
     * @return the number of dataSources
     */
    override fun count(): Long {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM jds_datasource", Long::class.java) ?: 0
    }

    /**
     * Returns all dataSources.
     *
     * @return all dataSources
     */
    override fun findAll(): Iterable<DataSource> {
        return jdbcTemplate.query("SELECT $dataSourceSelectColumns FROM jds_datasource", dataSourceRowMapper)
    }

    /**
     * Returns all instances of the dataSources with the given IDs.
     *
     * @param ids The ids
     * @return the dataSources
     */
    override fun findAllById(ids: Iterable<Long>): Iterable<DataSource> {
        return namedTemplate.query(
            "SELECT $dataSourceSelectColumns FROM jds_datasource WHERE id IN (:ids)",
            MapSqlParameterSource("ids", ids),
            dataSourceRowMapper)
    }

    /**
     * Return the list of data sources referencing the given [DataProvider] id
     *
     * @param dataProviderId The id of the data provider referenced
     * @return the list of data sources referencing the given data provider id
     */
    fun findByDataProviderId(dataProviderId: Long): List<DataSource> {
        return jdbcTemplate.query(
            "SELECT $dataSourceSelectColumns FROM jds_datasource WHERE data_provider_id = ?",
            arrayOf<Any>(dataProviderId),
            dataSourceRowMapper)
    }

    /**
     * Return the list of data sources referencing the given [DataProvider] ids
     *
     * @param dataProviderIds The id of the data providers referenced
     * @return the list of data sources referencing the given data provider ids
     */
    fun findByDataProviderIdIn(dataProviderIds: List<Long>): List<DataSource> {
        return namedTemplate.query(
            "SELECT $dataSourceSelectColumns FROM jds_datasource WHERE data_provider_id IN (:dataProviderIds)",
            MapSqlParameterSource("dataProviderIds", dataProviderIds),
            dataSourceRowMapper)
    }

    /**
     * Return the list of data sources having the given user id as a reader
     *
     * @param userId The id of the user
     * @return the list of data sources referencing the given user id
     */
    fun findByUserAllowedToReadIds(userId: Long): List<DataSource> {

        return jdbcTemplate.query(
            "SELECT $dataSourceSelectColumns FROM jds_datasource WHERE id IN (SELECT DISTINCT(data_source_id) FROM jds_datasource_user WHERE user_id = ?)",
            arrayOf<Any>(userId),
            dataSourceRowMapper)
    }

    /**
     * Returns whether an dataSource with the given id exists.
     *
     * @param id The id
     * @return {@literal true} if an dataSource with the given id exists, {@literal false} otherwise.
     */
    override fun existsById(id: Long): Boolean {
        return jdbcTemplate
            .queryForObject(
                "SELECT COUNT(*) FROM jds_datasource WHERE id = ?",
                arrayOf<Any>(id),
                Long::class.java)
            .let { it > 0 }
    }

    /**
     * Retrieves an dataSource by its id.
     *
     * @param id must not be {@literal null}.
     * @return the dataSource with the given id or {@literal Optional#empty()} if none found
     */
    override fun findById(id: Long): Optional<DataSource> {
        return Optional.ofNullable(
            jdbcTemplate.queryForObject(
                "SELECT $dataSourceSelectColumns FROM jds_datasource WHERE id = ?",
                arrayOf<Any>(id),
                dataSourceRowMapper))
    }

    /**
     * Saves a given dataSource. Use the returned dataSource for further operations as the save operation might have changed the
     * entity instance completely.
     *
     * @param dataSource The dataSource to save
     * @return the saved dataSource
     */
    override fun <S : DataSource> save(dataSource: S): S {

        return findById(upsert(dataSource)).get() as S
    }

    /**
     * Saves all given dataSources.
     *
     * @param dataSources The dataSources to save
     * @return the saved dataSources.
     */
    override fun <S : DataSource> saveAll(dataSources: Iterable<S>): Iterable<S> {

        // Upsert all the dataSources
        val ids = dataSources.map { upsert(it) }
        // Reload them
        return findAllById(ids) as Iterable<S>
    }

    /**
     * Deletes all entities managed by the repository.
     */
    override fun deleteAll() {
        jdbcTemplate.execute("TRUNCATE TABLE jds_datasource_user")
        jdbcTemplate.execute("DELETE FROM jds_datasource")
    }

    /**
     * Deletes the given dataSources.
     *
     * @param dataSources The dataSources to delete
     */
    override fun deleteAll(dataSources: Iterable<DataSource>) {

        val ids = dataSources.mapNotNull { it.id }

        namedTemplate.update(
            "DELETE FROM jds_datasource_user WHERE data_source_id IN (:ids)",
            MapSqlParameterSource("ids", ids))

        namedTemplate.update(
            "DELETE FROM jds_datasource WHERE id IN (:ids)",
            MapSqlParameterSource("ids", ids))
    }

    /**
     * Deletes a given dataSource.
     *
     * @param dataSource The dataSource to delete
     */
    override fun delete(dataSource: DataSource) {
        dataSource.id
            ?.let { dataSourceId ->

                jdbcTemplate.update(
                    "DELETE FROM jds_datasource_user WHERE data_source_id = ?",
                    arrayOf<Any>(dataSourceId))

                jdbcTemplate.update(
                    "DELETE FROM jds_datasource WHERE id = ?",
                    arrayOf<Any>(dataSourceId))
            }
            ?: throw IllegalArgumentException("Unable to delete a non persisted DataSource object")
    }

    /**
     * Deletes the dataSource with the given id.
     *
     * @param id The id to delete
     */
    override fun deleteById(id: Long) {

        jdbcTemplate.update(
            "DELETE FROM jds_datasource_user WHERE data_source_id = ?",
            arrayOf<Any>(id))

        jdbcTemplate.update(
            "DELETE FROM jds_datasource WHERE id = ?",
            arrayOf<Any>(id))
    }

    /**
     * Save an dataSource by creating it or updating it. If the given dataSource has an id, but this id does not exist in the
     * database, a new entry is created in the database, possibly with another id.
     *
     * @param dataSource The dataSource to delete
     * @return the id of the saved entity
     */
    private fun upsert(dataSource: DataSource): Long {

        val dataSourceId = dataSource.id
            ?.let { dataSourceId ->
                if (existsById(dataSourceId)) {
                    jdbcTemplate.update(
                        "UPDATE jds_datasource SET name = ?, data_provider_id = ? WHERE id = ?",
                        arrayOf(dataSource.name, dataSource.dataProviderId, dataSource.id),
                        arrayOf(java.sql.Types.VARCHAR, java.sql.Types.BIGINT, java.sql.Types.BIGINT).toIntArray())
                    dataSourceId
                }
                else {
                    val keyHolder = GeneratedKeyHolder()
                    jdbcTemplate.update(mapInsert(dataSource), keyHolder)
                    keyHolder.key as Long
                }
            }
            ?: run {
                val keyHolder = GeneratedKeyHolder()
                jdbcTemplate.update(mapInsert(dataSource), keyHolder)
                keyHolder.key as Long
            }

        // Delete old related users
        jdbcTemplate.update(
            "DELETE FROM jds_datasource_user WHERE data_source_id = ?",
            arrayOf<Any>(dataSourceId))

        val deleterIds = dataSource.userAllowedToDeleteIds.toList()
        val writerIds = (dataSource.userAllowedToWriteIds - dataSource.userAllowedToDeleteIds).toList()
        val readerIds = (dataSource.userAllowedToReadIds - dataSource.userAllowedToWriteIds).toList()

        // Save the users
        this.jdbcTemplate.batchUpdate(
            "INSERT INTO jds_datasource_user(data_source_id, user_id, permission) VALUES(?, ?, ?)",
            object : BatchPreparedStatementSetter {

                @Throws(SQLException::class)
                override fun setValues(ps: PreparedStatement, i: Int) {
                    ps.setLong(1, dataSourceId)

                    when {
                        i < deleterIds.size                  -> {
                            ps.setLong(2, deleterIds[i])
                            ps.setString(3, ALLOWED_DELETE)
                        }
                        i < deleterIds.size + writerIds.size -> {
                            ps.setLong(2, writerIds[i - deleterIds.size])
                            ps.setString(3, ALLOWED_WRITE)
                        }
                        else                                 -> {
                            ps.setLong(2, readerIds[i - writerIds.size - deleterIds.size])
                            ps.setString(3, ALLOWED_READ)
                        }
                    }
                }

                override fun getBatchSize(): Int {
                    return deleterIds.size + writerIds.size + readerIds.size
                }
            })

        return dataSourceId

    }

    /**
     * Create the mapping function returning the [PreparedStatement] for inserting an [DataSource]
     *
     * @param dataSource The dataSource to insert
     * @return a function creating a prepared statement from a connection
     */
    private fun mapInsert(dataSource: DataSource): ((Connection) -> PreparedStatement) {

        return { connection ->
            val ps = connection.prepareStatement("INSERT INTO jds_datasource(name, data_provider_id) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)
            ps.setString(1, dataSource.name)
            ps.setLong(2, dataSource.dataProviderId)
            ps
        }
    }

    /**
     * Class doing the mapping from a database entry to an [DataSource] object
     */
    inner class DataSourceRowMapper : RowMapper<DataSource> {

        /**
         * Map a single row of data given as a [ResultSet] to an [DataSource]
         * @param rs the ResultSet to map (pre-initialized for the current row)
         * @param rowNum the number of the current row
         * @return the DataSource object for the current row
         * @throws SQLException if a SQLException is encountered getting
         * column values (that is, there's no need to catch SQLException)
         */
        @Throws(SQLException::class)
        override fun mapRow(rs: ResultSet, rowNum: Int): DataSource {

            val id = rs.getLong(1)
            val name = rs.getString(2)
            val dataProviderId = rs.getLong(3)

            val userRights = jdbcTemplate.query(
                "SELECT $userRightSelectColumns FROM jds_datasource_user WHERE data_source_id = ?",
                arrayOf<Any>(id),
                userRightRowMapper)

            val deletersOrNo = userRights.partition { it.second == ALLOWED_DELETE }
            val writersOrNo = deletersOrNo.second.partition { it.second == ALLOWED_WRITE }

            val deleterIds = deletersOrNo.first.map { it.first }.toSet()
            val writersIds = writersOrNo.first.map { it.first }.toSet()
            val readersIds = writersOrNo.second.map { it.first }.toSet()

            return DataSource(
                id,
                name,
                dataProviderId,
                readersIds + writersIds + deleterIds,
                writersIds + deleterIds,
                deleterIds)
        }
    }

    /**
     * Class doing the mapping from a database entry to a Pair of userId and access right object
     */
    inner class UserRightRowMapper : RowMapper<Pair<Long, String>> {

        /**
         * Map a single row of data given as a [ResultSet] to a Pair of userId and access right object
         * @param rs the ResultSet to map (pre-initialized for the current row)
         * @param rowNum the number of the current row
         * @return a Pair of userId and access right object
         * @throws SQLException if a SQLException is encountered getting
         * column values (that is, there's no need to catch SQLException)
         */
        @Throws(SQLException::class)
        override fun mapRow(rs: ResultSet, rowNum: Int): Pair<Long, String> {

            val id = rs.getLong(1)
            val allowed = rs.getString(2)

            return Pair(id, allowed)
        }
    }
}
