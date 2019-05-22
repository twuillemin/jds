package net.wuillemin.jds.common.event

import org.springframework.context.ApplicationEvent

/**
 * Event that a Group has been updated
 *
 * @param groupId The object on which the event initially occurred
 */
class GroupUpdatedEvent(val groupId: Long) : ApplicationEvent(groupId)