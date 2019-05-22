package net.wuillemin.jds.dataserver.service.model

import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.common.exception.C
import net.wuillemin.jds.common.exception.ConstraintException
import net.wuillemin.jds.common.exception.NotFoundException
import net.wuillemin.jds.dataserver.entity.model.DataProvider
import net.wuillemin.jds.dataserver.entity.model.DataSource
import net.wuillemin.jds.dataserver.entity.model.Schema
import net.wuillemin.jds.dataserver.event.DataProviderCreatedEvent
import net.wuillemin.jds.dataserver.event.DataProviderDeletedEvent
import net.wuillemin.jds.dataserver.event.DataProviderUpdatedEvent
import net.wuillemin.jds.dataserver.exception.E
import net.wuillemin.jds.dataserver.repository.DataProviderRepository
import org.slf4j.Logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

/**
 * Service for managing the dataProviders
 *
 * @param dataProviderRepository The repository with the dataProvider configuration
 * @param dataSourceService The service for [DataSource]
 * @param applicationEventPublisher The application's events
 * @param logger The logger
 */
@Service
class DataProviderService(
    val dataProviderRepository: DataProviderRepository,
    val dataSourceService: DataSourceService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val logger: Logger
) {

    /**
     * Get the list of all dataProviders.
     *
     * @return the list of all dataProviders
     */
    fun getDataProviders(): List<DataProvider> {
        return dataProviderRepository.findAll().toList()
    }

    /**
     * Get the list of all data providers using the given [Schema].
     *
     * @param schema The [Schema] for which to retrieve the data providers
     * @return the data providers
     * @throws BadParameterException if the server parameter has it id property to null
     */
    fun getDataProvidersForSchema(schema: Schema): List<DataProvider> {

        return schema.id
            ?.let { schemaId ->
                dataProviderRepository.findAllBySchemaId(schemaId)
            }
            ?: throw BadParameterException(E.service.model.dataProvider.getForSchemaNotPersisted)

    }

    /**
     * Get the list of all data providers using the given [Schema].
     *
     * @param schemas The [Schema]s for which to retrieve the data providers
     * @return the data providers
     * @throws BadParameterException if the schema parameter has it id property to null
     */
    fun getDataProvidersForSchemas(schemas: List<Schema>): List<DataProvider> {

        val schemaIds = schemas.map { schema ->
            schema.id
                ?.let {
                    it
                }
                ?: throw BadParameterException(E.service.model.dataProvider.getForSchemasNotPersisted)

        }

        return dataProviderRepository.findAllBySchemaIdIn(schemaIds)
    }

    /**
     * Get a data provider by its id.
     *
     * @param id The id of the dataProvider
     * @return the dataProvider
     * @throws NotFoundException if the dataProvider does not exist
     */
    fun getDataProviderById(id: Long): DataProvider {
        return dataProviderRepository.findById(id).orElseThrow {
            NotFoundException(C.notFound.idClass, id, DataProvider::class)
        }
    }

    /**
     * Create a new data provider in the database. Send a [DataProviderCreatedEvent] in case of success.
     *
     * @param dataProvider The dataProvider to create
     * @return the dataProvider created
     * @throws BadParameterException if the dataProvider parameter has it id property not null
     */
    fun addDataProvider(dataProvider: DataProvider): DataProvider {

        return dataProvider.id
            ?.let {
                throw BadParameterException(E.service.model.dataProvider.createAlreadyPersisted)
            }
            ?: run {
                dataProviderRepository
                    .save(dataProvider)
                    .also { createdDataProvider ->
                        applicationEventPublisher.publishEvent(DataProviderCreatedEvent(createdDataProvider.id!!))
                        logger.info("addDataProvider: the DataProvider ${createdDataProvider.getLoggingId()} has been created")
                    }
            }
    }

    /**
     * Update a data provider by its its id. Send a [DataProviderUpdatedEvent] in case of success.
     *
     * @param dataProvider the new information for the dataProvider
     * @return the updated dataProvider
     * @throws NotFoundException if there is no existing data provider
     * @throws ConstraintException if the schemaId property is updated
     * @throws BadParameterException if the dataProvider parameter has it id property to null
     */
    fun updateDataProvider(dataProvider: DataProvider): DataProvider {

        return dataProvider.id
            ?.let { dataProviderId ->

                val existingDataProvider = getDataProviderById(dataProviderId)

                if (dataProvider.schemaId != existingDataProvider.schemaId) {
                    throw ConstraintException(E.service.model.dataProvider.updateSchemaId)
                }

                dataProviderRepository
                    .save(dataProvider)
                    .also {
                        applicationEventPublisher.publishEvent(DataProviderUpdatedEvent(dataProviderId))
                    }
            }
            ?: throw BadParameterException(E.service.model.dataProvider.updateNotPersisted)

    }

    /**
     * Delete a data provider. Send a [DataProviderDeletedEvent] in case of success.
     *
     * @param dataProvider The dataProvider delete
     * @throws ConstraintException if the dataProvider is referenced by one ore more [DataProvider]
     * @throws BadParameterException if the dataProvider parameter has it id property to null
     */
    fun deleteDataProvider(dataProvider: DataProvider) {

        dataProvider.id
            ?.let { dataProviderId ->

                if (dataSourceService.getDataSourcesForDataProvider(dataProvider).isNotEmpty()) {
                    throw ConstraintException(E.service.model.dataProvider.deleteReferenced, dataProvider)
                }

                dataProviderRepository.delete(dataProvider)

                applicationEventPublisher.publishEvent(DataProviderDeletedEvent(dataProviderId))

                logger.info("deleteDataProvider: the DataProvider ${dataProvider.getLoggingId()} has been deleted")
            }
            ?: throw BadParameterException(E.service.model.dataProvider.deleteNotPersisted)

    }
}
