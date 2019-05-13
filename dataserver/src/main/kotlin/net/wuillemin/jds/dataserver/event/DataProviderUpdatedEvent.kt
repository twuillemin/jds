package net.wuillemin.jds.dataserver.event

import org.springframework.context.ApplicationEvent

/**
 * Event that a DataProvider object has been updated
 *
 * @param dataProviderId The object on which the event initially occurred
 */
class DataProviderUpdatedEvent(val dataProviderId: String) : ApplicationEvent(dataProviderId)