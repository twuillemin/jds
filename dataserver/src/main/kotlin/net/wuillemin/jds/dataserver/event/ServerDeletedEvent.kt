package net.wuillemin.jds.dataserver.event

import org.springframework.context.ApplicationEvent

/**
 * Event that a Server object has been deleted
 *
 * @param serverId The object on which the event initially occurred
 */
class ServerDeletedEvent(val serverId: String) : ApplicationEvent(serverId)