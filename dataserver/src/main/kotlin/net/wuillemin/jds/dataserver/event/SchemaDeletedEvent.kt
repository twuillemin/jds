package net.wuillemin.jds.dataserver.event

import org.springframework.context.ApplicationEvent

/**
 * Event that a Schema object has been deleted
 *
 * @param schemaId The object on which the event initially occurred
 */
class SchemaDeletedEvent(val schemaId: Long) : ApplicationEvent(schemaId)