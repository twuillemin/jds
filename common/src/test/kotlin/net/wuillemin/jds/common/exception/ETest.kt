package net.wuillemin.jds.common.exception

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.MessageSource
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*
import kotlin.reflect.full.memberProperties


@ExtendWith(SpringExtension::class)
class ETest {

    @Test
    fun testE() {
        val bundler = ResourceBundleMessageSource()
        bundler.addBasenames("messages/common_messages")
        classVisitor(bundler, Locale.getDefault(), E)
    }

    @Test
    fun testEFrench() {
        val bundler = ResourceBundleMessageSource()
        bundler.addBasenames("messages/common_messages")
        classVisitor(bundler, Locale.FRENCH, E)
    }

    @Test
    fun testC() {
        val bundler = ResourceBundleMessageSource()
        bundler.addBasenames("messages/common_messages")
        classVisitor(bundler, Locale.getDefault(), C)
    }

    private fun <T : Any> classVisitor(messageSource: MessageSource, locale: Locale, toVisit: T) {

        if (toVisit is ExceptionCode) {
            messageSource.getMessage(toVisit.code, emptyArray(), locale)
        }
        else {
            System.out.println("checking: ${toVisit::class.simpleName}")

            toVisit::class.memberProperties.forEach { prop ->
                prop.getter.call(toVisit)
                    ?.let {
                        classVisitor(messageSource, locale, it)
                    }
                    ?: run {
                        Assertions.fail<Unit>("The property ${prop.name} returned a null value")
                    }
            }
        }
    }
}