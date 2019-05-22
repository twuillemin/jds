package net.wuillemin.jds.dataserver.event

import org.springframework.context.ApplicationEvent

/**
 * Event that a DataProvider object has been deleted
 *
 * @param dataProviderId The object on which the event initially occurred
 */
class DataProviderDeletedEvent(val dataProviderId: Long) : ApplicationEvent(dataProviderId)