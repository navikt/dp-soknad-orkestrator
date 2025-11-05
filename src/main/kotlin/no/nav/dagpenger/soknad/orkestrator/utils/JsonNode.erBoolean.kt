package no.nav.dagpenger.soknad.orkestrator.utils

import com.fasterxml.jackson.databind.JsonNode

fun JsonNode.erBoolean(): Boolean =
    when {
        this.asText().lowercase() == "ja" -> {
            true
        }
        this.asText().lowercase() == "nei" -> {
            false
        }
        else -> {
            this.asBoolean()
        }
    }
