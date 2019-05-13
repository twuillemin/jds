package net.wuillemin.jds.dataserver.event

import org.springframework.context.ApplicationEvent

/**
 * Event that a DataSource object has been created
 *
 * @param dataSourceId The object on which the event initially occurred
 */
class DataSourceCreatedEvent(val dataSourceId: String) : ApplicationEvent(dataSourceId)