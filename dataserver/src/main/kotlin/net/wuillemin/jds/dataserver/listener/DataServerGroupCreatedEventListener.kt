package net.wuillemin.jds.dataserver.listener

import net.wuillemin.jds.common.entity.Group
import net.wuillemin.jds.common.event.GroupCreatedEvent
import net.wuillemin.jds.common.service.GroupService
import net.wuillemin.jds.dataserver.service.importation.GroupInitializer
import org.slf4j.Logger
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

/**
 * Component receiving the GroupCreatedEvent
 *
 * @param groupInitializer The service for initialization environment
 * @param groupService The service managing [Group]
 * @param logger The Logger
 */
@Component
class DataServerGroupCreatedEventListener(
    private val groupInitializer: GroupInitializer,
    private val groupService: GroupService,
    private val logger: Logger
) : ApplicationListener<GroupCreatedEvent> {

    /**
     * Process the event
     * @param event The event
     */
    override fun onApplicationEvent(event: GroupCreatedEvent) {
        logger.debug("onApplicationEvent: received ${event.groupId}")

        val group = groupService.getGroupById(event.groupId)

        groupInitializer.createSQLEnvironmentForGroup(group)

    }
}