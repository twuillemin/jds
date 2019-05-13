package net.wuillemin.jds.dataserver.event

import org.springframework.context.ApplicationEvent

/**
 * Event that a DataProvider object has been created
 *
 * @param dataProviderId The object on which the event initially occurred
 */
class DataProviderCreatedEvent(val dataProviderId: String) : ApplicationEvent(dataProviderId)