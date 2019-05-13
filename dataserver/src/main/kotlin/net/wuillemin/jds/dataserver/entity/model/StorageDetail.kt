package net.wuillemin.jds.dataserver.entity.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Represents the information needed to insert a column inside a container
 *
 * @param readAttributeName The name of the attribute which is returned when read from the storage. For example, in SQL
 * this may be the name of the column in upper case
 * @param nullable If the column is Nullable
 * @param primaryKey If the column is a Primary Key
 * @param autoIncrement If the column is autoIncremented
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = WritableStorage::class, name = "writable"),
    JsonSubTypes.Type(value = ReadOnlyStorage::class, name = "readOnly"))
sealed class StorageDetail(
    open val readAttributeName: String,
    open val nullable: Boolean,
    open val primaryKey: Boolean,
    open val autoIncrement: Boolean
)

/**
 * Represents a container that is writable

 * @param containerName The name of the container in which the column could be inserted. For example, for SQL
 * this is the name of the table
 * @param writeAttributeName The name of the attribute which must be used when writing in the container. In the general
 * case, this is the same as the readAttributeName
 * this may be the name of the column in upper case
 */
data class WritableStorage(
    val containerName: String,
    override val readAttributeName: String,
    val writeAttributeName: String,
    override val nullable: Boolean,
    override val primaryKey: Boolean,
    override val autoIncrement: Boolean
) : StorageDetail(readAttributeName, nullable, primaryKey, autoIncrement)

/**
 * Represents a container that is read only
 */
data class ReadOnlyStorage(
    override val readAttributeName: String,
    override val nullable: Boolean,
    override val primaryKey: Boolean,
    override val autoIncrement: Boolean
) : StorageDetail(readAttributeName, nullable, primaryKey, autoIncrement)

