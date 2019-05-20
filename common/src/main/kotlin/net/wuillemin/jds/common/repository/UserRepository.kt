package net.wuillemin.jds.common.repository

import net.wuillemin.jds.common.entity.Profile
import net.wuillemin.jds.common.entity.User
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
 * The repository of the [User]s that are stored in the database
 */
@Suppress("SqlResolve")
@Repository
class UserRepository(@Qualifier("commonJdbcTemplate") private val jdbcTemplate: JdbcTemplate) : CrudRepository<User, Long> {


    private val rowMapperColumns = "id, name, password, first_name, last_name, enabled, profile"

    private val rowMapper = UserRowMapper()

    private val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate.dataSource!!)


    /**
     * Returns the number of users available.
     *
     * @return the number of users
     */
    override fun count(): Long {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM jds_user", Long::class.java) ?: 0
    }

    /**
     * Returns all users.
     *
     * @return all users
     */
    override fun findAll(): Iterable<User> {
        return jdbcTemplate.query("SELECT $rowMapperColumns FROM jds_user", rowMapper)
    }

    /**
     * Returns all instances of the users with the given IDs.
     *
     * @param ids The ids
     * @return the users
     */
    override fun findAllById(ids: Iterable<Long>): Iterable<User> {
        return namedTemplate.query(
            "SELECT $rowMapperColumns FROM jds_user WHERE id IN (:ids)",
            MapSqlParameterSource("ids", ids),
            rowMapper)
    }

    /**
     * Search a user by its name.
     *
     * @param name The name to search
     * @return a list of user object, that should contain 1 object is the user is found
     */
    fun findAllByName(name: String): List<User> {
        return jdbcTemplate.query(
            "SELECT $rowMapperColumns FROM jds_user WHERE name = ?",
            arrayOf<Any>(name),
            rowMapper)
    }

    /**
     * Returns whether an user with the given id exists.
     *
     * @param id The id
     * @return {@literal true} if an user with the given id exists, {@literal false} otherwise.
     */
    override fun existsById(id: Long): Boolean {
        return jdbcTemplate
            .queryForObject(
                "SELECT COUNT(*) FROM jds_user WHERE id = ?",
                arrayOf<Any>(id),
                Long::class.java)
            .let { it > 0 }
    }

    /**
     * Retrieves an user by its id.
     *
     * @param id must not be {@literal null}.
     * @return the user with the given id or {@literal Optional#empty()} if none found
     */
    override fun findById(id: Long): Optional<User> {
        return Optional.ofNullable(
            jdbcTemplate.queryForObject(
                "SELECT $rowMapperColumns FROM jds_user WHERE id = ?",
                arrayOf<Any>(id),
                rowMapper))
    }

    /**
     * Saves a given user. Use the returned user for further operations as the save operation might have changed the
     * entity instance completely.
     *
     * @param user The user to save
     * @return the saved user
     */
    override fun <S : User> save(user: S): S {

        return findById(upsert(user)).get() as S
    }

    /**
     * Saves all given users.
     *
     * @param users The users to save
     * @return the saved users.
     */
    override fun <S : User> saveAll(users: Iterable<S>): Iterable<S> {

        // Upsert all the users
        val ids = users.map { upsert(it) }
        // Reload them
        return findAllById(ids) as Iterable<S>
    }

    /**
     * Deletes all entities managed by the repository.
     */
    override fun deleteAll() {
        jdbcTemplate.execute("TRUNCATE TABLE jds_group_user")
        jdbcTemplate.execute("DELETE FROM jds_user")
    }

    /**
     * Deletes the given users.
     *
     * @param users The users to delete
     */
    override fun deleteAll(users: Iterable<User>) {
        namedTemplate.update(
            "DELETE FROM jds_group_user WHERE user_id IN (:ids)",
            MapSqlParameterSource("ids", users.mapNotNull { it.id }))

        namedTemplate.update(
            "DELETE FROM jds_user WHERE id IN (:ids)",
            MapSqlParameterSource("ids", users.mapNotNull { it.id }))
    }

    /**
     * Deletes a given user.
     *
     * @param user The user to delete
     */
    override fun delete(user: User) {
        user.id
            ?.let { userId ->

                jdbcTemplate.update(
                    "DELETE FROM jds_group_user WHERE user_id = ?",
                    arrayOf<Any>(userId),
                    arrayOf(java.sql.Types.BIGINT).toIntArray())

                jdbcTemplate.update(
                    "DELETE FROM jds_user WHERE id = ?",
                    arrayOf<Any>(userId),
                    arrayOf(java.sql.Types.BIGINT).toIntArray())
            }
            ?: throw IllegalArgumentException("Unable to delete a non persisted User object")
    }

    /**
     * Deletes the user with the given id.
     *
     * @param id The id to delete
     */
    override fun deleteById(id: Long) {
        jdbcTemplate.update(
            "DELETE FROM jds_group_user WHERE user_id = ?",
            arrayOf<Any>(id),
            arrayOf(java.sql.Types.BIGINT).toIntArray())

        jdbcTemplate.update(
            "DELETE FROM jds_user WHERE id = ?",
            arrayOf<Any>(id),
            arrayOf(java.sql.Types.BIGINT).toIntArray())
    }

    /**
     * Save an user by creating it or updating it. If the given user has an id, but this id does not exist in the
     * database, a new entry is created in the database, possibly with another id.
     *
     * @param user The user to delete
     * @return the id of the saved entity
     */
    private fun upsert(user: User): Long {

        return user.id
            ?.let { userId ->
                if (existsById(userId)) {
                    jdbcTemplate.update(
                        "UPDATE jds_user SET name = ?, password = ?, first_name = ?, last_name = ?, enabled = ?, profile = ? WHERE id = ?",
                        arrayOf(user.name, user.password, user.firstName, user.lastName, user.enabled, user.profile.toString(), userId),
                        arrayOf(java.sql.Types.VARCHAR, java.sql.Types.VARCHAR, java.sql.Types.VARCHAR, java.sql.Types.VARCHAR, java.sql.Types.BOOLEAN, java.sql.Types.VARCHAR, java.sql.Types.BIGINT).toIntArray())
                    userId
                }
                else {
                    val keyHolder = GeneratedKeyHolder()
                    jdbcTemplate.update(mapInsert(user), keyHolder)
                    keyHolder.key as Long
                }
            }
            ?: run {
                val keyHolder = GeneratedKeyHolder()
                jdbcTemplate.update(mapInsert(user), keyHolder)
                keyHolder.key as Long
            }
    }

    /**
     * Create the mapping function returning the [PreparedStatement] for inserting an [User]
     *
     * @param user The user to insert
     * @return a function creating a prepared statement from a connection
     */
    private fun mapInsert(user: User): ((Connection) -> PreparedStatement) {

        return { connection ->
            val ps = connection.prepareStatement("INSERT INTO jds_user(name, password, first_name, last_name, enabled, profile) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
            ps.setString(1, user.name)
            ps.setString(2, user.password)
            ps.setString(3, user.firstName)
            ps.setString(4, user.lastName)
            ps.setBoolean(5, user.enabled)
            ps.setString(6, user.profile.toString())
            ps
        }
    }

    /**
     * Class doing the mapping from a database entry to an [User] object
     */
    inner class UserRowMapper : RowMapper<User> {

        /**
         * Map a single row of data given as a [ResultSet] to an [User]
         * @param rs the ResultSet to map (pre-initialized for the current row)
         * @param rowNum the number of the current row
         * @return the User object for the current row (may be {@code null})
         * @throws SQLException if a SQLException is encountered getting
         * column values (that is, there's no need to catch SQLException)
         */
        @Throws(SQLException::class)
        override fun mapRow(rs: ResultSet, rowNum: Int): User {
            return User(
                rs.getLong(1),
                rs.getString(2),
                rs.getString(3),
                rs.getString(4),
                rs.getString(5),
                rs.getBoolean(6),
                Profile.valueOf(rs.getString(7)))
        }
    }
}