package net.wuillemin.jds.common.event

import org.springframework.context.ApplicationEvent

/**
 * Event that a Group has been created
 *
 * @param groupId The object on which the event initially occurred
 */
class GroupCreatedEvent(val groupId: Long) : ApplicationEvent(groupId)