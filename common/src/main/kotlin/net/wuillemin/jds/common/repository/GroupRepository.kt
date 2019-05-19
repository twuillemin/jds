package net.wuillemin.jds.common.repository

import net.wuillemin.jds.common.entity.Group
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


/**
 * The repository of the groups that are stored in the database
 */
@Suppress("SqlResolve")
@Repository
class GroupRepository(@Qualifier("commonJdbcTemplate") private val jdbcTemplate: JdbcTemplate) : CrudRepository<Group, Long> {


    private val rowMapperColumns = "id, name"

    private val rowMapper = GroupRowMapper()

    private val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate.dataSource!!)


    /**
     * Returns the number of groups available.
     *
     * @return the number of groups
     */
    override fun count(): Long {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM jds_group", Long::class.java) ?: 0
    }

    /**
     * Returns all groups.
     *
     * @return all groups
     */
    override fun findAll(): Iterable<Group> {
        return jdbcTemplate.query("SELECT $rowMapperColumns FROM jds_group", rowMapper)
    }

    /**
     * Returns all instances of the groups with the given IDs.
     *
     * @param ids The ids
     * @return the groups
     */
    override fun findAllById(ids: Iterable<Long>): Iterable<Group> {
        return namedTemplate.query(
            "SELECT $rowMapperColumns FROM jds_group WHERE id IN (:ids)",
            MapSqlParameterSource("ids", ids),
            rowMapper)
    }

    /**
     * Search a group by its name.
     *
     * @param name The name to search
     * @return a list of group object, that should contain 1 object is the group is found
     */
    fun findAllByName(name: String): List<Group> {
        return jdbcTemplate.query(
            "SELECT $rowMapperColumns FROM jds_group WHERE name = ?",
            arrayOf<Any>(name),
            rowMapper)
    }

    /**
     * Search all groups related to the given user id by its name.
     *
     * @param name The name to search
     * @return a list of group object, that should contain 1 object is the group is found
     */
    fun findAllByUserId(userId: Long): List<Group> {
        return jdbcTemplate.query(
            "SELECT $rowMapperColumns FROM jds_group WHERE id IN (SELECT distinct(group_id) FROM jds_group_user WHERE user_id = ?)",
            arrayOf<Any>(userId),
            rowMapper)
    }

    /**
     * Returns whether a group with the given id exists.
     *
     * @param id The id
     * @return {@literal true} if a group with the given id exists, {@literal false} otherwise.
     */
    override fun existsById(id: Long): Boolean {
        return jdbcTemplate
            .queryForObject(
                "SELECT COUNT(*) FROM jds_group WHERE id = ?",
                arrayOf<Any>(id),
                Long::class.java)
            .let { it > 0 }
    }

    /**
     * Retrieves a group by its id.
     *
     * @param id must not be {@literal null}.
     * @return the group with the given id or {@literal Optional#empty()} if none found
     */
    override fun findById(id: Long): Optional<Group> {
        return Optional.ofNullable(
            jdbcTemplate.queryForObject(
                "SELECT $rowMapperColumns FROM jds_group WHERE id = ?",
                arrayOf<Any>(id),
                rowMapper))
    }

    /**
     * Saves a given group. Use the returned group for further operations as the save operation might have changed the
     * entity instance completely.
     *
     * @param group The group to save
     * @return the saved group
     */
    override fun <S : Group> save(group: S): S {

        return findById(upsert(group)).get() as S
    }

    /**
     * Saves all given groups.
     *
     * @param groups The groups to save
     * @return the saved groups.
     */
    override fun <S : Group> saveAll(groups: Iterable<S>): Iterable<S> {

        // Upsert all the groups
        val ids = groups.map { upsert(it) }
        // Reload them
        return findAllById(ids) as Iterable<S>
    }

    /**
     * Deletes all entities managed by the repository.
     */
    override fun deleteAll() {
        jdbcTemplate.execute("TRUNCATE TABLE jds_group")
    }

    /**
     * Deletes the given groups.
     *
     * @param groups The groups to delete
     */
    override fun deleteAll(groups: Iterable<Group>) {
        namedTemplate.update(
            "DELETE FROM jds_group WHERE id IN (:ids)",
            MapSqlParameterSource("ids", groups.mapNotNull { it.id }))
    }

    /**
     * Deletes a given group.
     *
     * @param group The group to delete
     */
    override fun delete(group: Group) {
        group.id
            ?.let { groupId ->
                jdbcTemplate.update(
                    "DELETE FROM jds_group WHERE id = ?",
                    arrayOf<Any>(groupId))
            }
            ?: throw IllegalArgumentException("Unable to delete a non persisted Group object")
    }

    /**
     * Deletes the group with the given id.
     *
     * @param id The id to delete
     */
    override fun deleteById(id: Long) {
        jdbcTemplate.update(
            "DELETE FROM jds_group WHERE id = ?",
            arrayOf<Any>(id))
    }

    /**
     * Save an group by creating it or updating it. If the given group has an id, but this id does not exist in the
     * database, a new entry is created in the database, possibly with another id.
     *
     * @param group The group to delete
     * @return the id of the saved entity
     */
    private fun upsert(group: Group): Long {

        // Create or update the group
        val groupId = group.id
            ?.let { groupId ->
                if (existsById(groupId)) {
                    jdbcTemplate.update(
                        "UPDATE jds_group SET name = ? WHERE id = ?",
                        arrayOf(group.name, groupId),
                        arrayOf(java.sql.Types.VARCHAR, java.sql.Types.BIGINT).toIntArray())
                    groupId
                }
                else {
                    val keyHolder = GeneratedKeyHolder()
                    jdbcTemplate.update(mapInsert(group), keyHolder)
                    keyHolder.key as Long
                }
            }
            ?: run {
                val keyHolder = GeneratedKeyHolder()
                jdbcTemplate.update(mapInsert(group), keyHolder)
                keyHolder.key as Long
            }

        // Delete old relations
        jdbcTemplate.update(
            "DELETE FROM jds_group_user WHERE group_id = ?",
            arrayOf<Any>(groupId))

        val adminIds = group.administratorIds.toList()
        val userIds = (group.userIds - group.administratorIds).toList()

        // Insert the admin and the users
        this.jdbcTemplate.batchUpdate(
            "INSERT INTO jds_group_user(group_id, user_id, is_admin) VALUES(?, ?, ?)",
            object : BatchPreparedStatementSetter {

                @Throws(SQLException::class)
                override fun setValues(ps: PreparedStatement, i: Int) {
                    ps.setLong(1, groupId)
                    if (i < adminIds.size) {
                        ps.setLong(2, adminIds[i])
                        ps.setBoolean(3, true)
                    }
                    else {
                        ps.setLong(2, userIds[i - adminIds.size])
                        ps.setBoolean(3, false)
                    }
                }

                override fun getBatchSize(): Int {
                    return adminIds.size + userIds.size
                }
            })

        return groupId
    }

    /**
     * Create the mapping function returning the [PreparedStatement] for inserting an [Group]
     *
     * @param group The group to insert
     * @return a function creating a prepared statement from a connection
     */
    private fun mapInsert(group: Group): ((Connection) -> PreparedStatement) {

        return { connection ->
            val ps = connection.prepareStatement("INSERT INTO jds_group(name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)
            ps.setString(1, group.name)
            ps
        }
    }

    /**
     * Class doing the mapping from a database entry to an [Group] object
     */
    inner class GroupRowMapper : RowMapper<Group> {

        /**
         * Map a single row of data given as a [ResultSet] to an [Group]
         * @param rs the ResultSet to map (pre-initialized for the current row)
         * @param rowNum the number of the current row
         * @return the Group object for the current row (may be {@code null})
         * @throws SQLException if a SQLException is encountered getting
         * column values (that is, there's no need to catch SQLException)
         */
        @Throws(SQLException::class)
        override fun mapRow(rs: ResultSet, rowNum: Int): Group {

            val id = rs.getLong(1)
            val name = rs.getString(2)

            val adminIds = jdbcTemplate
                .queryForList(
                    "SELECT user_id FROM jds_group_user WHERE group_id = ? AND is_admin = 1",
                    arrayOf<Any>(id),
                    Long::class.java)
                .toSet()

            val userIds = jdbcTemplate
                .queryForList(
                    "SELECT user_id FROM jds_group_user WHERE group_id = ?",
                    arrayOf<Any>(id),
                    Long::class.java)
                .toSet()

            return Group(
                id,
                name,
                adminIds,
                userIds)
        }
    }
}