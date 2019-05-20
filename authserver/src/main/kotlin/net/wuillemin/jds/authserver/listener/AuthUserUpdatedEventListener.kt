package net.wuillemin.jds.authserver.listener

import net.wuillemin.jds.authserver.service.TokenGenerator
import net.wuillemin.jds.common.event.UserUpdatedEvent
import org.slf4j.Logger
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

/**
 * Component receiving the UserUpdatedEvent
 *
 * @param tokenGenerator The service managing tokens
 * @param logger The Logger
 */
@Component
class AuthUserUpdatedEventListener(
    private val tokenGenerator: TokenGenerator,
    private val logger: Logger
) : ApplicationListener<UserUpdatedEvent> {

    /**
     * Process the event
     * @param event The event
     */
    override fun onApplicationEvent(event: UserUpdatedEvent) {
        logger.debug("onApplicationEvent: received ${event.userId}")
        tokenGenerator.updateUserPermission(event.userId)
    }
}