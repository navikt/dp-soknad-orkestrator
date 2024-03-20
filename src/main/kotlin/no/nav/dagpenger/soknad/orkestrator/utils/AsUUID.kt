package no.nav.dagpenger.soknad.orkestrator.utils

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

internal fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }
