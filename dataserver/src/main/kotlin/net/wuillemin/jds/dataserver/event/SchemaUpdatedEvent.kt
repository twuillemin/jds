package net.wuillemin.jds.dataserver.event

import org.springframework.context.ApplicationEvent

/**
 * Event that a Schema object has been updated
 *
 * @param schemaId The object on which the event initially occurred
 */
class SchemaUpdatedEvent(val schemaId: String) : ApplicationEvent(schemaId)