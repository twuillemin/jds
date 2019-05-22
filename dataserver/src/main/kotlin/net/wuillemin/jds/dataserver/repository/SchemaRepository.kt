package net.wuillemin.jds.dataserver.repository

import net.wuillemin.jds.dataserver.entity.model.Schema
import net.wuillemin.jds.dataserver.entity.model.SchemaGSheet
import net.wuillemin.jds.dataserver.entity.model.SchemaSQL
import net.wuillemin.jds.dataserver.entity.model.Server
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.data.repository.CrudRepository
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
private const val TYPE_SQL = "sql"
private const val TYPE_GSHEET = "gsheet"

/**
 * The repository for [Schema] objects
 */
@Suppress("SqlResolve")
@Repository
class SchemaRepository(
    @Qualifier("dataserverJdbcTemplate") private val jdbcTemplate: JdbcTemplate,
    private val dataProviderRepository: DataProviderRepository
) : CrudRepository<Schema, Long> {

    private val schemaSelectColumns = "id, type, name, server_id, sql_role_name"

    private val schemaRowMapper = SchemaRowMapper()

    private val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate.dataSource!!)

    /**
     * Returns the number of schemas available.
     *
     * @return the number of schemas
     */
    override fun count(): Long {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM jds_schema", Long::class.java) ?: 0
    }

    /**
     * Returns all schemas.
     *
     * @return all schemas
     */
    override fun findAll(): List<Schema> {
        return jdbcTemplate.query("SELECT $schemaSelectColumns FROM jds_schema", schemaRowMapper)
    }

    /**
     * Returns all instances of the schemas with the given IDs.
     *
     * @param ids The ids
     * @return the schemas
     */
    override fun findAllById(ids: Iterable<Long>): List<Schema> {
        return namedTemplate.query(
            "SELECT $schemaSelectColumns FROM jds_schema WHERE id IN (:ids)",
            MapSqlParameterSource("ids", ids),
            schemaRowMapper)
    }

    /**
     * Return the list of schemas referencing the given [Server] id.
     *
     * @param serverId The id of the server referenced
     * @return the list of schema referencing the giver server id
     */
    fun findAllByServerId(serverId: Long): List<Schema> {
        return jdbcTemplate.query(
            "SELECT $schemaSelectColumns FROM jds_schema WHERE server_id = ?",
            arrayOf<Any>(serverId),
            schemaRowMapper)
    }

    /**
     * Return the list of schemas referencing the given [Server] ids.
     *
     * @param serverIds The list of ids of the server referenced
     * @return the list of schema referencing the giver server ids
     */
    fun findAllByServerIdIn(serverIds: List<Long>): List<Schema> {
        return namedTemplate.query(
            "SELECT $schemaSelectColumns FROM jds_schema WHERE server_id IN (:serverIds)",
            MapSqlParameterSource("serverIds", serverIds),
            schemaRowMapper)
    }

    /**
     * Returns whether an schema with the given id exists.
     *
     * @param id The id
     * @return {@literal true} if an schema with the given id exists, {@literal false} otherwise.
     */
    override fun existsById(id: Long): Boolean {
        return jdbcTemplate
            .queryForObject(
                "SELECT COUNT(*) FROM jds_schema WHERE id = ?",
                arrayOf<Any>(id),
                Long::class.java)
            .let { it > 0 }
    }

    /**
     * Retrieves an schema by its id.
     *
     * @param id The id of the schema to retrieve.
     * @return the schema with the given id or {@literal Optional#empty()} if none found
     */
    override fun findById(id: Long): Optional<Schema> {
        return try {
            return Optional.ofNullable(
                jdbcTemplate.queryForObject(
                    "SELECT $schemaSelectColumns FROM jds_schema WHERE id = ?",
                    arrayOf<Any>(id),
                    schemaRowMapper))
        }
        catch (e: EmptyResultDataAccessException) {
            Optional.empty()
        }
    }

    /**
     * Saves a given schema. Use the returned schema for further operations as the save operation might have changed the
     * entity instance completely.
     *
     * @param schema The schema to save
     * @return the saved schema
     */
    override fun <S : Schema> save(schema: S): S {

        return findById(upsert(schema)).get() as S
    }

    /**
     * Saves all given schemas.
     *
     * @param schemas The schemas to save
     * @return the saved schemas.
     */
    override fun <S : Schema> saveAll(schemas: Iterable<S>): Iterable<S> {

        // Upsert all the schemas
        val ids = schemas.map { upsert(it) }
        // Reload them
        return findAllById(ids) as Iterable<S>
    }

    /**
     * Deletes all entities managed by the repository.
     */
    override fun deleteAll() {
        dataProviderRepository.deleteAll()
        jdbcTemplate.execute("DELETE FROM jds_schema")
    }

    /**
     * Deletes the given schemas.
     *
     * @param schemas The schemas to delete
     */
    override fun deleteAll(schemas: Iterable<Schema>) {

        val schemaIds = schemas.mapNotNull { it.id }

        dataProviderRepository.deleteAll(dataProviderRepository.findAllBySchemaIdIn(schemaIds))

        namedTemplate.update(
            "DELETE FROM jds_schema WHERE id IN (:ids)",
            MapSqlParameterSource("ids", schemaIds))
    }

    /**
     * Deletes a given schema.
     *
     * @param schema The schema to delete
     */
    override fun delete(schema: Schema) {
        schema.id
            ?.let { schemaId -> deleteById(schemaId) }
            ?: throw IllegalArgumentException("Unable to delete a non persisted Schema object")
    }

    /**
     * Deletes the schema with the given id.
     *
     * @param id The id to delete
     */
    override fun deleteById(id: Long) {

        dataProviderRepository.deleteAll(dataProviderRepository.findAllBySchemaId(id))

        jdbcTemplate.update("DELETE FROM jds_schema WHERE id = ?", id)
    }

    /**
     * Save an schema by creating it or updating it. If the given schema has an id, but this id does not exist in the
     * database, a new entry is created in the database, possibly with another id.
     *
     * @param schema The schema to delete
     * @return the id of the saved entity
     */
    private fun upsert(schema: Schema): Long {

        return schema.id
            ?.let { schemaId ->
                if (existsById(schemaId)) {

                    val args = when (schema) {
                        is SchemaSQL    -> arrayOf(
                            TYPE_SQL,
                            schema.name,
                            schema.serverId,
                            schema.roleName,
                            schema.id)
                        is SchemaGSheet -> arrayOf(
                            TYPE_GSHEET,
                            schema.name,
                            schema.serverId,
                            null,
                            schema.id)
                    }

                    jdbcTemplate.update(
                        "UPDATE jds_schema SET type = ?, name = ?, server_id = ?, sql_role_name = ? WHERE id = ?",
                        args,
                        arrayOf(
                            java.sql.Types.VARCHAR,
                            java.sql.Types.VARCHAR,
                            java.sql.Types.BIGINT,
                            java.sql.Types.VARCHAR,
                            java.sql.Types.BIGINT).toIntArray())
                    schemaId
                }
                else {
                    val keyHolder = GeneratedKeyHolder()
                    jdbcTemplate.update(mapInsert(schema), keyHolder)
                    keyHolder.key as Long
                }
            }
            ?: run {
                val keyHolder = GeneratedKeyHolder()
                jdbcTemplate.update(mapInsert(schema), keyHolder)
                keyHolder.key as Long
            }
    }

    /**
     * Create the mapping function returning the [PreparedStatement] for inserting an [Schema]
     *
     * @param schema The schema to insert
     * @return a function creating a prepared statement from a connection
     */
    private fun mapInsert(schema: Schema): ((Connection) -> PreparedStatement) {

        return { connection ->
            val ps = connection.prepareStatement("INSERT INTO jds_schema(type, name, server_id, sql_role_name) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)

            when (schema) {
                is SchemaSQL    -> {
                    ps.setString(1, TYPE_SQL)
                    ps.setString(2, schema.name)
                    ps.setLong(3, schema.serverId)
                    ps.setString(4, schema.roleName)
                }
                is SchemaGSheet -> {
                    ps.setString(1, TYPE_GSHEET)
                    ps.setString(2, schema.name)
                    ps.setLong(3, schema.serverId)
                    ps.setNull(4, java.sql.Types.VARCHAR)
                }
            }
            ps
        }
    }

    /**
     * Class doing the mapping from a database entry to an [Schema] object
     */
    inner class SchemaRowMapper : RowMapper<Schema> {

        /**
         * Map a single row of data given as a [ResultSet] to an [Schema]
         * @param rs the ResultSet to map (pre-initialized for the current row)
         * @param rowNum the number of the current row
         * @return the Schema object for the current row (may be {@code null})
         * @throws SQLException if a SQLException is encountered getting
         * column values (that is, there's no need to catch SQLException)
         */
        @Throws(SQLException::class)
        override fun mapRow(rs: ResultSet, rowNum: Int): Schema {

            val id = rs.getLong(1)
            val type = rs.getString(2)
            val name = rs.getString(3)
            val serverId = rs.getLong(4)

            return when (type) {
                TYPE_SQL    -> SchemaSQL(
                    id,
                    name,
                    serverId,
                    rs.getString(5)
                )
                TYPE_GSHEET -> SchemaGSheet(
                    id,
                    name,
                    serverId
                )
                else        -> throw SQLException("Unknown Schema type when reading from database \"$type\"")
            }
        }
    }
}
