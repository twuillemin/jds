package net.wuillemin.jds.common.event

import org.springframework.context.ApplicationEvent

/**
 * Event that an User has been updated
 *
 * @param userId The object on which the event initially occurred
 */
class UserUpdatedEvent(val userId: Long) : ApplicationEvent(userId)