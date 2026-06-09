package no.nav.dagpenger.soknad.orkestrator.utils

import tools.jackson.databind.JsonNode
import java.util.UUID

internal fun JsonNode.asUUID(): UUID = this.asString().let { UUID.fromString(it) }
