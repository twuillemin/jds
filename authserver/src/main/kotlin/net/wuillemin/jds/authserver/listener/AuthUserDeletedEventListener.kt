package net.wuillemin.jds.authserver.listener

import net.wuillemin.jds.authserver.service.TokenGenerator
import net.wuillemin.jds.common.event.UserDeletedEvent
import org.slf4j.Logger
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

/**
 * Component receiving the UserDeletedEvent
 *
 * @param tokenGenerator The service managing tokens
 * @param logger The Logger
 */
@Component
class AuthUserDeletedEventListener(
    private val tokenGenerator: TokenGenerator,
    private val logger: Logger) : ApplicationListener<UserDeletedEvent> {

    /**
     * Process the event
     * @param event The event
     */
    override fun onApplicationEvent(event: UserDeletedEvent) {
        logger.debug("onApplicationEvent: received ${event.userId}")
        tokenGenerator.logOutAllUserSessions(event.userId)
    }
}