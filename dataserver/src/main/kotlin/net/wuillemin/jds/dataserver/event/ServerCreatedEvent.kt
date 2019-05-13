package net.wuillemin.jds.dataserver.event

import org.springframework.context.ApplicationEvent

/**
 * Event that a Server object has been created
 *
 * @param serverId The object on which the event initially occurred
 */
class ServerCreatedEvent(val serverId: String) : ApplicationEvent(serverId)