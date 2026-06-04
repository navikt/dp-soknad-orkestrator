package no.nav.dagpenger.soknad.orkestrator.utils

import tools.jackson.databind.JsonNode
import java.util.UUID

internal fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }
