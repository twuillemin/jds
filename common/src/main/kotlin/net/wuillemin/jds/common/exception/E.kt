package net.wuillemin.jds.common.exception

/**
 * Declaration of exception for the common package
 */
@Suppress("KDocMissingDocumentation", "PublicApiImplicitType")
object E {

    val controller = Controller()
    val security = Security()
    val service = Service()

    class Controller {

        val group = Group()
        val user = User()

        class Group {
            val getDenied = ExceptionCode("common.controller.group.get_denied")
            val setAdministratorsDenied = ExceptionCode("common.controller.group.set_administrators_denied")
            val setUsersDenied = ExceptionCode("common.controller.group.set_users_denied")
            val deleteDenied = ExceptionCode("common.controller.group.delete_denied")
        }

        class User {
            val getDenied = ExceptionCode("common.controller.user.get_denied")
            val updateDenied = ExceptionCode("common.controller.user.update_denied")
            val updateDifferentIds = ExceptionCode("common.controller.user.update_different_ids")
            val deleteDenied = ExceptionCode("common.controller.user.delete_denied")
        }
    }

    class Security {

        val authenticatorJWT = AuthenticatorJWT()
        val jwtFilter = JwtFilter()

        class AuthenticatorJWT {
            val noResponse = ExceptionCode("common.security.authenticatorjwt.no_response")
        }

        class JwtFilter {
            val expired = ExceptionCode("common.security.jwtfilter.expired")
            val malformedToken = ExceptionCode("common.security.jwtfilter.malformed_token")
            val rolesMissing = ExceptionCode("common.security.jwtfilter.malformed_roles_missing")
            val rolesNotList = ExceptionCode("common.security.jwtfilter.malformed_roles_list")
            val rolesEmpty = ExceptionCode("common.security.jwtfilter.malformed_roles_empty")
            val permissionMissing = ExceptionCode("common.security.jwtfilter.malformed_permission_missing")
            val permissionNotMap = ExceptionCode("common.security.jwtfilter.malformed_permission_map")
            val permissionMissingUserId = ExceptionCode("common.security.jwtfilter.malformed_permission_userid")
            val permissionMissingAdmin = ExceptionCode("common.security.jwtfilter.malformed_permission_admin")
            val permissionMissingUser = ExceptionCode("common.security.jwtfilter.malformed_permission_user")
            val permissionOther = ExceptionCode("common.security.jwtfilter.malformed_permission_other")
        }
    }

    class Service {

        val group = Group()
        val user = User()
        val localization = Localization()
        val jsonConnector = JsonConnector()

        class Group {
            val getMultipleGroupsSameName = ExceptionCode("common.service.group.get_by_name_multiple")
            val createAlreadyPersisted = ExceptionCode("common.service.group.create_already_persisted")
            val createNameAlreadyExists = ExceptionCode("common.service.group.create_name_already_exists")
            val createCreatorNotPersisted = ExceptionCode("common.service.group.create_creator_not_persisted")
            val setUsersWithUsersNotPersisted = ExceptionCode("common.service.group.setusers_users_not_persisted")
            val setUsersWithGroupNotPersisted = ExceptionCode("common.service.group.setusers_group_not_persisted")
            val setAdministratorsWithUsersNotPersisted = ExceptionCode("common.service.group.setadministrators_users_not_persisted")
            val setAdministratorsNoAdministratorLeft = ExceptionCode("common.service.group.setadministrators_no_administrator_left")
            val setAdministratorsWithGroupNotPersisted = ExceptionCode("common.service.group.setadministrators_group_not_persisted")
            val removeUserWithUserNotPersisted = ExceptionCode("common.service.group.removeuser_user_not_persisted")
            val removeUserNoAdministratorLeft = ExceptionCode("common.service.group.removeuser_no_administrator_left")
            val removeUserWithGroupNotPersisted = ExceptionCode("common.service.group.removeuser_group_not_persisted")
            val deleteGroupNotPersisted = ExceptionCode("common.service.group.delete_group_not_persisted")
        }

        class User {
            val getMultipleUsersSameName = ExceptionCode("common.service.user.get_by_name_multiple")
            val createAlreadyPersisted = ExceptionCode("common.service.user.create_already_persisted")
            val createNameAlreadyExists = ExceptionCode("common.service.user.create_name_already_exists")
            val createUserWithGroupsNotEmpty = ExceptionCode("common.service.user.create_groups_not_empty")
            val updateNotPersisted = ExceptionCode("common.service.user.update_not_persisted")
            val updateWithModifiedGroups = ExceptionCode("common.service.user.update_group_modified")
            val updateNameAlreadyExists = ExceptionCode("common.service.user.update_username_not_available")
            val deleteNoAdministratorLeft = ExceptionCode("common.service.user.delete_no_administrator_left")
            val deleteUserNotPersisted = ExceptionCode("common.service.user.delete_user_not_persisted")
        }

        class Localization {
            val unknownCode = ExceptionCode("common.service.localization.unknown_code")
        }

        class JsonConnector {
            val emptyResponse = ExceptionCode("common.service.jsonconnector.empty_response")
            val retrieveHttpException = ExceptionCode("common.service.jsonconnector.retrieve_http_exception")
            val retrieveRestException = ExceptionCode("common.service.jsonconnector.retrieve_rest_exception")
            val retrieveOtherException = ExceptionCode("common.service.jsonconnector.retrieve_other_exception")
            val deleteHttpException = ExceptionCode("common.service.jsonconnector.delete_http_exception")
            val deleteRestException = ExceptionCode("common.service.jsonconnector.delete_rest_exception")
            val deleteOtherException = ExceptionCode("common.service.jsonconnector.delete_other_exception")
        }
    }
}