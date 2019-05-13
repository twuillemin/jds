package net.wuillemin.jds.common.event

import org.springframework.context.ApplicationEvent

/**
 * Event that an User has been created
 *
 * @param userId The object on which the event initially occurred
 */
class UserCreatedEvent(val userId: String) : ApplicationEvent(userId)