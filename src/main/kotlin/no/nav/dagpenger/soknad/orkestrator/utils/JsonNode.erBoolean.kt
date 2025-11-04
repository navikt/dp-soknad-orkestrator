package no.nav.dagpenger.soknad.orkestrator.utils

import com.fasterxml.jackson.databind.JsonNode

fun JsonNode.erBoolean(): Boolean {
    if (this.asText().lowercase() == "ja") {
        return true
    } else if (this.asText().lowercase() == "nei") {
        return false
    } else {
        return this.asBoolean()
    }
}
