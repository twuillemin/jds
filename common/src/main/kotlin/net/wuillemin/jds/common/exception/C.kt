package net.wuillemin.jds.common.exception

/**
 * Declaration of exception in the common package that are shared by the whole application
 */
@Suppress("KDocMissingDocumentation", "PublicApiImplicitType")
object C {

    val notFound = NotFound()

    class NotFound {
        val idClass = ExceptionCode("common.exception.not_found_id_class")
        val valueAttributeClass: ExceptionCode = ExceptionCode("common.exception.not_found_value_attribute_class")
    }
}