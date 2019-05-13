package net.wuillemin.jds.authserver.exception

import net.wuillemin.jds.common.exception.ExceptionCode

@Suppress("KDocMissingDocumentation", "PublicApiImplicitType")
object E {

    val controller = Controller()
    val service = Service()

    class Controller {

        val external = External()

        class External {
            val noEmail = ExceptionCode("authserver.controller.external.no_email")
            val googleNoResponse = ExceptionCode("authserver.controller.external.google_no_response")
            val googleOtherError = ExceptionCode("authserver.controller.external.google_other_error")
        }
    }

    class Service {

        val permissionBuilder = PermissionBuilder()
        val tokenGenerator = TokenGenerator()

        class PermissionBuilder {
            val incoherentPermission = ExceptionCode("authserver.service.permission_builder.incoherent_permission")
            val userNotPersisted = ExceptionCode("authserver.service.permission_builder.user_not_persisted")
        }

        class TokenGenerator {
            val malformedToken = ExceptionCode("authserver.service.token_generator.malformed_token")
            val tokenNotFound = ExceptionCode("authserver.service.token_generator.token_not_found")
            val tokenExpired = ExceptionCode("authserver.service.token_generator.token_expired")
        }
    }
}