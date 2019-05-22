package net.wuillemin.jds.dataserver.event

import org.springframework.context.ApplicationEvent

/**
 * Event that a DataSource object has been deleted
 *
 * @param dataSourceId The object on which the event initially occurred
 */
class DataSourceDeletedEvent(val dataSourceId: Long) : ApplicationEvent(dataSourceId)