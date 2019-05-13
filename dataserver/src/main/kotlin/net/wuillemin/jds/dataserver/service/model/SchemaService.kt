package net.wuillemin.jds.dataserver.service.model

import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.common.exception.C
import net.wuillemin.jds.common.exception.ConstraintException
import net.wuillemin.jds.common.exception.NotFoundException
import net.wuillemin.jds.dataserver.entity.model.DataProvider
import net.wuillemin.jds.dataserver.entity.model.Schema
import net.wuillemin.jds.dataserver.entity.model.Server
import net.wuillemin.jds.dataserver.event.SchemaCreatedEvent
import net.wuillemin.jds.dataserver.event.SchemaDeletedEvent
import net.wuillemin.jds.dataserver.event.SchemaUpdatedEvent
import net.wuillemin.jds.dataserver.exception.E
import net.wuillemin.jds.dataserver.repository.SchemaRepository
import org.slf4j.Logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

/**
 * Service for managing the schemas
 *
 * @param schemaRepository The repository with the schema configuration
 * @param dataProviderService The service for [DataProvider]
 * @param applicationEventPublisher The application's events
 * @param logger The logger
 */
@Service
class SchemaService(
    private val schemaRepository: SchemaRepository,
    private val dataProviderService: DataProviderService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val logger: Logger) {

    /**
     * Get the list of all schemas.
     *
     * @return the list of all schemas
     */
    fun getSchemas(): List<Schema> {
        return schemaRepository.findAll().toList()
    }

    /**
     * Get the list of all schemas using the given [Server].
     *
     * @param server The server for which to retrieve the schemas
     * @return the schemas
     * @throws BadParameterException if the server parameter has it id property to null
     */
    fun getSchemasForServer(server: Server): List<Schema> {

        return server.id
            ?.let { serverId ->
                schemaRepository.findByServerId(serverId).toList()
            }
            ?: throw BadParameterException(E.service.model.schema.getForServerNoId)

    }

    /**
     * Get the list of all schemas using the given [Server]s.
     *
     * @param servers The servers for which to retrieve the schemas
     * @return the schemas
     * @throws BadParameterException if one of the servers parameter has it id property to null
     */
    fun getSchemasForServers(servers: List<Server>): List<Schema> {

        val serverIds = servers.map { server ->
            server.id
                ?.let {
                    it
                }
                ?: throw BadParameterException(E.service.model.schema.getForServersNoId)

        }

        return schemaRepository.findByServerIdIn(serverIds)
    }

    /**
     * Get a schema by its id.
     *
     * @param id The id of the schema
     * @return the schema
     * @throws NotFoundException if the schema does not exist
     */
    fun getSchemaById(id: String): Schema {
        return schemaRepository.findById(id).orElseThrow {
            NotFoundException(C.notFound.idClass, id, Schema::class)
        }
    }

    /**
     * Create a new schema in the database. Send a [SchemaCreatedEvent] in case of success.
     *
     * @param schema The schema to create
     * @return the schema created
     * @throws BadParameterException if the schema parameter has it id property not null
     */
    fun addSchema(schema: Schema): Schema {

        return schema.id
            ?.let {
                throw BadParameterException(E.service.model.schema.createAlreadyPersisted)
            }
            ?: run {
                schemaRepository
                    .save(schema)
                    .also { createdSchema ->
                        applicationEventPublisher.publishEvent(SchemaCreatedEvent(createdSchema.id!!))
                        logger.info("addSchema: The schema ${createdSchema.getLoggingId()} has been created")
                    }
            }
    }

    /**
     * Update a schema by its its id. Send a [SchemaUpdatedEvent] in case of success.
     *
     * @param schema the new information for the schema
     * @return the updated schema
     * @throws NotFoundException if there is no existing schema
     * @throws ConstraintException if the serverId property is updated
     * @throws BadParameterException if the schema parameter has it id property to null
     */
    fun updateSchema(schema: Schema): Schema {

        return schema.id
            ?.let { schemaId ->

                val existingSchema = getSchemaById(schemaId)

                if (existingSchema.serverId != schema.serverId) {
                    throw ConstraintException(E.service.model.schema.updateServerId)
                }

                schemaRepository
                    .save(schema)
                    .also {
                        applicationEventPublisher.publishEvent(SchemaUpdatedEvent(schemaId))
                    }
            }
            ?: throw BadParameterException(E.service.model.schema.updateNotPersisted)

    }

    /**
     * Delete a schema. Send a [SchemaDeletedEvent] in case of success.
     *
     * @param schema The schema delete
     * @throws ConstraintException if the schema is referenced by one ore more [DataProvider]
     * @throws BadParameterException if the schema parameter has it id property to null
     */
    fun deleteSchema(schema: Schema) {

        schema.id
            ?.let { schemaId ->

                if (dataProviderService.getDataProvidersForSchema(schema).isNotEmpty()) {
                    throw ConstraintException(E.service.model.schema.deleteReferenced, schema)
                }

                schemaRepository.delete(schema)

                applicationEventPublisher.publishEvent(SchemaDeletedEvent(schemaId))

                logger.info("deleteSchema: The schema ${schema.getLoggingId()} has been deleted")
            }
            ?: throw BadParameterException(E.service.model.schema.deleteNotPersisted)

    }
}
