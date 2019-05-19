package net.wuillemin.jds.dataserver.repository

import net.wuillemin.jds.dataserver.entity.model.DataProvider
import net.wuillemin.jds.dataserver.entity.model.DataSource
import org.springframework.data.repository.CrudRepository

/**
 * The repository for [DataSource] objects
 */
interface DataSourceRepository : CrudRepository<DataSource, Long> {

    /**
     * Return the list of data sources referencing the given [DataProvider] id
     *
     * @param dataProviderId The id of the data provider referenced
     * @return the list of data sources referencing the given data provider id
     */
    fun findByDataProviderId(dataProviderId: Long): List<DataSource>

    /**
     * Return the list of data sources referencing the given [DataProvider] ids
     *
     * @param dataProviderIds The id of the data providers referenced
     * @return the list of data sources referencing the given data provider ids
     */
    fun findByDataProviderIdIn(dataProviderIds: List<Long>): List<DataSource>

    /**
     * Return the list of data sources having the given user id as a reader
     *
     * @param userId The id of the user
     * @return the list of data sources referencing the given user id
     */
    fun findByUserAllowedToReadIds(userId: Long): List<DataSource>
}