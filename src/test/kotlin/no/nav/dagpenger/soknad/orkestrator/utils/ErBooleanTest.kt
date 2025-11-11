package no.nav.dagpenger.soknad.orkestrator.utils

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test

class ErBooleanTest {
    val jaNode =
        com.fasterxml.jackson.databind
            .ObjectMapper()
            .readTree("\"ja\"")
    val neiNode =
        com.fasterxml.jackson.databind
            .ObjectMapper()
            .readTree("\"nei\"")
    val trueNode =
        com.fasterxml.jackson.databind
            .ObjectMapper()
            .readTree("true")
    val falseNode =
        com.fasterxml.jackson.databind
            .ObjectMapper()
            .readTree("false")

    @Test
    fun testErBoolean() {
        assertTrue(jaNode.erBoolean())
        assertFalse(neiNode.erBoolean())
        assertTrue(trueNode.erBoolean())
        assertFalse(falseNode.erBoolean())
    }
}
