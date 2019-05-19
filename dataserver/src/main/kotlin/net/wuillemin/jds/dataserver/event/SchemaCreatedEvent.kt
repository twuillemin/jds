package net.wuillemin.jds.dataserver.event

import org.springframework.context.ApplicationEvent

/**
 * Event that a Schema object has been created
 *
 * @param schemaId The object on which the event initially occurred
 */
class SchemaCreatedEvent(val schemaId: Long) : ApplicationEvent(schemaId)