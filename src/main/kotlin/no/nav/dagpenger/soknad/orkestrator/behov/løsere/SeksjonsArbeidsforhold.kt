package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import tools.jackson.databind.JsonNode

// Tolker ett element i registrerteArbeidsforhold fra seksjonen "arbeidsforhold" (ny søknad-flyt).
internal fun JsonNode.erPermittert() = this["hvordanHarDetteArbeidsforholdetEndretSeg"]?.asString() == "jegErPermittert"

internal fun JsonNode.erFiskepermittert() =
    erPermittert() &&
        this["permittertErDuPermittertFraFiskeforedlingsEllerFiskeoljeindustrien"]?.asString() == "ja"
