package net.wuillemin.jds.dataserver.repository

import net.wuillemin.jds.dataserver.entity.model.DataProvider
import net.wuillemin.jds.dataserver.entity.model.Schema
import org.springframework.data.mongodb.repository.MongoRepository

/**
 * The repository for [DataProvider] objects
 */
interface DataProviderRepository : MongoRepository<DataProvider, String> {

    /**
     * Return the list of dataProviders referencing the given [Schema] id
     *
     * @param schemaId The id of the schema referenced
     * @return the list of data providers referencing the given schema id
     */
    fun findBySchemaId(schemaId: String): List<DataProvider>

    /**
     * Return the list of dataProviders referencing the given [Schema] id
     *
     * @param schemaIds The id of the schemas referenced
     * @return the list of data providers referencing the given schema ids
     */
    fun findBySchemaIdIn(schemaIds: List<String>): List<DataProvider>

}