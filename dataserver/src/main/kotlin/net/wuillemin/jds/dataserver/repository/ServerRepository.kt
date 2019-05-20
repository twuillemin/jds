package net.wuillemin.jds.dataserver.repository

import net.wuillemin.jds.dataserver.entity.model.Server
import net.wuillemin.jds.dataserver.entity.model.ServerGSheet
import net.wuillemin.jds.dataserver.entity.model.ServerSQL
import org.springframework.beans.factory.annotation.Qualifier
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

/**
 * The repository for [Server] objects
 */
@Suppress("SqlResolve")
@Repository
class ServerRepository(@Qualifier("dataserverJdbcTemplate") private val jdbcTemplate: JdbcTemplate) : CrudRepository<Server, Long> {

    private val serverSelectColumns = "id, type, name, group_id, customer_defined, sql_jdbc_url, sql_user_name, sql_password, sql_driver_class_name, gsheet_workbook_url, gsheet_user_name, gsheet_password"

    private val serverRowMapper = ServerRowMapper()

    private val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate.dataSource!!)

    private val TYPE_SQL = "sql"
    private val TYPE_GSHEET = "gsheet"


    /**
     * Returns the number of servers available.
     *
     * @return the number of servers
     */
    override fun count(): Long {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM jds_server", Long::class.java) ?: 0
    }

    /**
     * Returns all servers.
     *
     * @return all servers
     */
    override fun findAll(): Iterable<Server> {
        return jdbcTemplate.query("SELECT $serverSelectColumns FROM jds_server", serverRowMapper)
    }

    /**
     * Returns all instances of the servers with the given IDs.
     *
     * @param ids The ids
     * @return the servers
     */
    override fun findAllById(ids: Iterable<Long>): Iterable<Server> {
        return namedTemplate.query(
            "SELECT $serverSelectColumns FROM jds_server WHERE id IN (:ids)",
            MapSqlParameterSource("ids", ids),
            serverRowMapper)
    }

    /**
     * Return the list of all servers related to a group
     * @param groupIds The id of the groups
     * @return a list of servers
     */
    fun findByGroupIdIn(groupIds: Iterable<Long>): Iterable<Server> {
        return namedTemplate.query(
            "SELECT $serverSelectColumns FROM jds_server WHERE group_id IN (:groupIds)",
            MapSqlParameterSource("groupIds", groupIds),
            serverRowMapper)
    }

    /**
     * Returns whether an server with the given id exists.
     *
     * @param id The id
     * @return {@literal true} if an server with the given id exists, {@literal false} otherwise.
     */
    override fun existsById(id: Long): Boolean {
        return jdbcTemplate
            .queryForObject(
                "SELECT COUNT(*) FROM jds_server WHERE id = ?",
                arrayOf<Any>(id),
                Long::class.java)
            .let { it > 0 }
    }

    /**
     * Retrieves an server by its id.
     *
     * @param id The id of the server to retrieve.
     * @return the server with the given id or {@literal Optional#empty()} if none found
     */
    override fun findById(id: Long): Optional<Server> {
        return Optional.ofNullable(
            jdbcTemplate.queryForObject(
                "SELECT $serverSelectColumns FROM jds_server WHERE id = ?",
                arrayOf<Any>(id),
                serverRowMapper))
    }

    /**
     * Saves a given server. Use the returned server for further operations as the save operation might have changed the
     * entity instance completely.
     *
     * @param server The server to save
     * @return the saved server
     */
    override fun <S : Server> save(server: S): S {

        return findById(upsert(server)).get() as S
    }

    /**
     * Saves all given servers.
     *
     * @param servers The servers to save
     * @return the saved servers.
     */
    override fun <S : Server> saveAll(servers: Iterable<S>): Iterable<S> {

        // Upsert all the servers
        val ids = servers.map { upsert(it) }
        // Reload them
        return findAllById(ids) as Iterable<S>
    }

    /**
     * Deletes all entities managed by the repository.
     */
    override fun deleteAll() {
        jdbcTemplate.execute("TRUNCATE TABLE jds_server")
    }

    /**
     * Deletes the given servers.
     *
     * @param servers The servers to delete
     */
    override fun deleteAll(servers: Iterable<Server>) {
        namedTemplate.update(
            "DELETE FROM jds_server WHERE id IN (:ids)",
            MapSqlParameterSource("ids", servers.mapNotNull { it.id }))
    }

    /**
     * Deletes a given server.
     *
     * @param server The server to delete
     */
    override fun delete(server: Server) {
        server.id
            ?.let { serverId ->
                jdbcTemplate.update(
                    "DELETE FROM jds_server WHERE id = ?",
                    arrayOf<Any>(serverId))
            }
            ?: throw IllegalArgumentException("Unable to delete a non persisted Server object")
    }

    /**
     * Deletes the server with the given id.
     *
     * @param id The id to delete
     */
    override fun deleteById(id: Long) {
        jdbcTemplate.update(
            "DELETE FROM jds_server WHERE id = ?",
            arrayOf<Any>(id))
    }

    /**
     * Save an server by creating it or updating it. If the given server has an id, but this id does not exist in the
     * database, a new entry is created in the database, possibly with another id.
     *
     * @param server The server to delete
     * @return the id of the saved entity
     */
    private fun upsert(server: Server): Long {

        return server.id
            ?.let { serverId ->
                if (existsById(serverId)) {

                    val args = when (server) {
                        is ServerSQL    -> arrayOf(
                            TYPE_SQL,
                            server.name,
                            server.groupId,
                            server.customerDefined,
                            server.jdbcURL,
                            server.userName,
                            server.password,
                            server.driverClassName,
                            null,
                            null,
                            null,
                            server.id)
                        is ServerGSheet -> arrayOf(
                            TYPE_GSHEET,
                            server.name,
                            server.groupId,
                            server.customerDefined,
                            null,
                            null,
                            null,
                            null,
                            server.workbookURL,
                            server.userName,
                            server.password,
                            server.id)
                    }

                    jdbcTemplate.update(
                        "UPDATE jds_server SET type = ?, name = ?, group_id = ?, customer_defined = ?, sql_jdbc_url = ?, sql_user_name = ?, sql_password = ?, sql_driver_class_name = ?, gsheet_workbook_url = ?, gsheet_user_name = ?, gsheet_password = ? WHERE id = ?",
                        args,
                        arrayOf(
                            java.sql.Types.VARCHAR,
                            java.sql.Types.VARCHAR,
                            java.sql.Types.BIGINT,
                            java.sql.Types.BOOLEAN,
                            java.sql.Types.VARCHAR,
                            java.sql.Types.VARCHAR,
                            java.sql.Types.VARCHAR,
                            java.sql.Types.VARCHAR,
                            java.sql.Types.VARCHAR,
                            java.sql.Types.VARCHAR,
                            java.sql.Types.VARCHAR,
                            java.sql.Types.BIGINT).toIntArray())
                    serverId
                }
                else {
                    val keyHolder = GeneratedKeyHolder()
                    jdbcTemplate.update(mapInsert(server), keyHolder)
                    keyHolder.key as Long
                }
            }
            ?: run {
                val keyHolder = GeneratedKeyHolder()
                jdbcTemplate.update(mapInsert(server), keyHolder)
                keyHolder.key as Long
            }
    }

    /**
     * Create the mapping function returning the [PreparedStatement] for inserting an [Server]
     *
     * @param server The server to insert
     * @return a function creating a prepared statement from a connection
     */
    private fun mapInsert(server: Server): ((Connection) -> PreparedStatement) {

        return { connection ->
            val ps = connection.prepareStatement("INSERT INTO jds_server(type, name, group_id, customer_defined, sql_jdbc_url, sql_user_name, sql_password, sql_driver_class_name, gsheet_workbook_url, gsheet_user_name, gsheet_password) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)

            when (server) {
                is ServerSQL    -> {
                    ps.setString(1, TYPE_SQL)
                    ps.setString(2, server.name)
                    ps.setLong(3, server.groupId)
                    ps.setBoolean(4, server.customerDefined)
                    ps.setString(5, server.jdbcURL)
                    ps.setString(6, server.userName)
                    ps.setString(7, server.password)
                    ps.setString(8, server.driverClassName)
                    ps.setNull(9, java.sql.Types.VARCHAR)
                    ps.setNull(10, java.sql.Types.VARCHAR)
                    ps.setNull(11, java.sql.Types.VARCHAR)
                }
                is ServerGSheet -> {
                    ps.setString(1, TYPE_SQL)
                    ps.setString(2, server.name)
                    ps.setLong(3, server.groupId)
                    ps.setBoolean(4, server.customerDefined)
                    ps.setNull(5, java.sql.Types.VARCHAR)
                    ps.setNull(6, java.sql.Types.VARCHAR)
                    ps.setNull(7, java.sql.Types.VARCHAR)
                    ps.setNull(8, java.sql.Types.VARCHAR)
                    ps.setString(9, server.workbookURL)
                    ps.setString(10, server.userName)
                    ps.setString(11, server.password)
                }
            }
            ps
        }
    }

    /**
     * Class doing the mapping from a database entry to an [Server] object
     */
    inner class ServerRowMapper : RowMapper<Server> {

        /**
         * Map a single row of data given as a [ResultSet] to an [Server]
         * @param rs the ResultSet to map (pre-initialized for the current row)
         * @param rowNum the number of the current row
         * @return the Server object for the current row (may be {@code null})
         * @throws SQLException if a SQLException is encountered getting
         * column values (that is, there's no need to catch SQLException)
         */
        @Throws(SQLException::class)
        override fun mapRow(rs: ResultSet, rowNum: Int): Server {

            val id = rs.getLong(1)
            val type = rs.getString(2)
            val name = rs.getString(3)
            val groupId = rs.getLong(4)
            val customerDefined = rs.getBoolean(5)

            return when (type) {
                TYPE_SQL    -> ServerSQL(
                    id,
                    name,
                    groupId,
                    customerDefined,
                    rs.getString(6),
                    rs.getString(7),
                    rs.getString(8),
                    rs.getString(9)
                )
                TYPE_GSHEET -> ServerGSheet(
                    id,
                    name,
                    groupId,
                    customerDefined,
                    rs.getString(10),
                    rs.getString(11),
                    rs.getString(12)
                )
                else        -> throw SQLException("Unknown Server type when reading from database \"$type\"")
            }
        }
    }
}