package net.wuillemin.jds.dataserver.service.model

import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.common.exception.C
import net.wuillemin.jds.common.exception.ConstraintException
import net.wuillemin.jds.common.exception.NotFoundException
import net.wuillemin.jds.dataserver.entity.model.DataProvider
import net.wuillemin.jds.dataserver.entity.model.DataSource
import net.wuillemin.jds.dataserver.event.DataSourceCreatedEvent
import net.wuillemin.jds.dataserver.event.DataSourceDeletedEvent
import net.wuillemin.jds.dataserver.event.DataSourceUpdatedEvent
import net.wuillemin.jds.dataserver.exception.E
import net.wuillemin.jds.dataserver.repository.DataSourceRepository
import org.slf4j.Logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

/**
 * Service for managing the dataSources (the basic supplier of data)
 *
 * @param dataSourceRepository The repository with the dataSource configuration
 * @param applicationEventPublisher The application's events
 * @param logger The logger
 */
@Service
class DataSourceService(
    val dataSourceRepository: DataSourceRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val logger: Logger
) {

    /**
     * Get the list of all dataSources.
     *
     * @return the list of all dataSources
     */
    fun getDataSources(): List<DataSource> {
        return dataSourceRepository.findAll().toList()
    }

    /**
     * Get the list of all data sources using the given [DataProvider].
     *
     * @param dataProvider The [DataProvider] for which to retrieve the data sources
     * @return the data sources
     * @throws BadParameterException if the server parameter has it id property to null
     */
    fun getDataSourcesForDataProvider(dataProvider: DataProvider): List<DataSource> {

        return dataProvider.id
            ?.let { dataProviderId ->
                dataSourceRepository.findAllByDataProviderId(dataProviderId)
            }
            ?: throw BadParameterException(E.service.model.dataSource.getForDataProviderNoId)
    }

    /**
     * Get the list of all data sources using the given [DataProvider].
     *
     * @param dataProviders The [DataProvider]s for which to retrieve the data sources
     * @return the data sources
     * @throws BadParameterException if the schema parameter has it id property to null
     */
    fun getDataSourcesForDataProviders(dataProviders: List<DataProvider>): List<DataSource> {

        val dataProviderIds = dataProviders.map { dataProvider ->
            dataProvider.id
                ?.let {
                    it
                }
                ?: throw BadParameterException(E.service.model.dataSource.getForDataProvidersNoId)

        }

        return dataSourceRepository.findAllByDataProviderIdIn(dataProviderIds)
    }

    /**
     * Get the list of all data sources having the given user id as a reader.
     *
     * @param userId The id of the user that have read access on the data source
     * @return the data sources
     */
    fun getDataSourcesForReaderId(userId: Long): List<DataSource> {
        return dataSourceRepository.findAllByUserAllowedToReadId(userId)
    }

    /**
     * Get a dataSource by its id.
     *
     * @param id The id of the dataSource
     * @return the dataSource
     * @throws NotFoundException if the dataSource does not exist
     */
    fun getDataSourceById(id: Long): DataSource {
        return dataSourceRepository.findById(id).orElseThrow {
            NotFoundException(C.notFound.idClass, id, DataSource::class)
        }
    }

    /**
     * Get a list of data sources by their ids
     *
     * @param ids The ids of the dataSource
     * @return the dataSources
     */
    fun getDataSourceByIds(ids: List<Long>): List<DataSource> {
        return dataSourceRepository.findAllById(ids).toList()
    }

    /**
     * Create a new dataSource in the database. Send a [DataSourceCreatedEvent] in case of success.
     *
     * @param dataSource The dataSource to create
     * @return the dataSource created
     * @throws BadParameterException if the dataSource parameter has it id property not null
     */
    fun addDataSource(dataSource: DataSource): DataSource {

        return dataSource.id
            ?.let {
                throw BadParameterException(E.service.model.dataSource.createAlreadyPersisted)
            }
            ?: run {
                dataSourceRepository
                    .save(ensureUsers(dataSource))
                    .also { createdDataSource ->
                        applicationEventPublisher.publishEvent(DataSourceCreatedEvent(createdDataSource.id!!))
                        logger.info("addDataSource: The data source ${createdDataSource.getLoggingId()} has been created")
                    }
            }
    }

    /**
     * Update a dataSource. Send a [DataSourceUpdatedEvent] in case of success.
     *
     * @param dataSource the new information for the dataSource
     * @return the updated dataSource
     * @throws NotFoundException if there is no existing data source
     * @throws ConstraintException if the dataProviderId property is updated
     * @throws BadParameterException if the dataSource parameter has it id property to null
     */
    fun updateDataSource(dataSource: DataSource): DataSource {

        return dataSource.id
            ?.let { dataSourceId ->

                val existingDataSource = getDataSourceById(dataSourceId)

                if (dataSource.dataProviderId != existingDataSource.dataProviderId) {
                    throw ConstraintException(E.service.model.dataSource.updateDataProviderId)
                }

                dataSourceRepository
                    .save(ensureUsers(dataSource))
                    .also {
                        applicationEventPublisher.publishEvent(DataSourceUpdatedEvent(dataSourceId))
                    }
            }
            ?: throw BadParameterException(E.service.model.dataSource.updateNotPersisted)

    }

    /**
     * Delete a dataSource. Send a [DataSourceDeletedEvent] in case of success.
     *
     * @param dataSource The dataSource delete
     * @throws ConstraintException if the dataSource is referenced by one ore more [DataSource]
     * @throws BadParameterException if the dataSource parameter has it id property to null
     */
    fun deleteDataSource(dataSource: DataSource) {

        dataSource.id
            ?.let { dataSourceId ->

                // TODO Add message to request if can delete

                dataSourceRepository.delete(dataSource)

                applicationEventPublisher.publishEvent(DataSourceDeletedEvent(dataSourceId))

                logger.info("deleteDataSource: the DataSource ${dataSource.getLoggingId()} has been deleted")
            }
            ?: throw BadParameterException(E.service.model.dataSource.deleteNotPersisted)

    }

    /**
     * Ensure that the users are correctly defined
     *
     * @param dataSource The data source the ensure
     * @return a new data source with users well defined
     */
    private fun ensureUsers(dataSource: DataSource): DataSource {

        return dataSource.copy(
            userAllowedToWriteIds = dataSource.userAllowedToWriteIds + dataSource.userAllowedToDeleteIds,
            userAllowedToReadIds = dataSource.userAllowedToReadIds + dataSource.userAllowedToWriteIds + dataSource.userAllowedToDeleteIds)
    }
}
