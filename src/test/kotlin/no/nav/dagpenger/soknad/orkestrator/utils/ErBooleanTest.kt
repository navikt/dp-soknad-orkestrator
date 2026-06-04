package no.nav.dagpenger.soknad.orkestrator.utils

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test

class ErBooleanTest {
    val jaNode =
        tools.jackson.databind.json
            .JsonMapper()
            .readTree("\"ja\"")
    val neiNode =
        tools.jackson.databind.json
            .JsonMapper()
            .readTree("\"nei\"")
    val trueNode =
        tools.jackson.databind.json
            .JsonMapper()
            .readTree("true")
    val falseNode =
        tools.jackson.databind.json
            .JsonMapper()
            .readTree("false")

    @Test
    fun testErBoolean() {
        assertTrue(jaNode.erBoolean())
        assertFalse(neiNode.erBoolean())
        assertTrue(trueNode.erBoolean())
        assertFalse(falseNode.erBoolean())
    }
}
