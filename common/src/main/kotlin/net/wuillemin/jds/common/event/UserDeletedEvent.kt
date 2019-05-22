package net.wuillemin.jds.common.event

import org.springframework.context.ApplicationEvent

/**
 * Event that an User has been deleted
 *
 * @param userId The object on which the event initially occurred
 */
class UserDeletedEvent(val userId: Long) : ApplicationEvent(userId)