package no.nav.dagpenger.soknad.orkestrator.utils

import tools.jackson.databind.JsonNode

fun JsonNode.erBoolean(): Boolean =
    when {
        this.asText().lowercase() == "ja" -> {
            true
        }

        this.asText().lowercase() == "nei" -> {
            false
        }

        this.isBoolean -> {
            this.booleanValue()
        }

        else -> {
            // Jackson 3 kaster for strenger som ikke er "true"/"false".
            // Matcher Jackson 2-atferd: kun eksakt "true" gir true.
            this.asText().lowercase() == "true"
        }
    }
