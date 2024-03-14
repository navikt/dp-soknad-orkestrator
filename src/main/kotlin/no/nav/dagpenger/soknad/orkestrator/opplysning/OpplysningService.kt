package no.nav.dagpenger.soknad.orkestrator.opplysning

import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

class OpplysningService(private val rapid: RapidsConnection, private val repository: OpplysningRepository) {
    fun løsBehov(behov: List<String>): Map<String, Any> {
        return behov.associateWith {
            when (it) {
                "Søknadstidspunkt" -> "todo"
                "JobbetUtenforNorge" -> "todo"
                "ØnskerDagpengerFraDato" -> "todo"
                "EøsArbeid" -> "todo"
                "KanJobbeDeltid" -> "todo"
                "HelseTilAlleTyperJobb" -> "todo"
                "KanJobbeHvorSomHelst" -> "todo"
                "VilligTilÅBytteYrke" -> "todo"
                else -> throw IllegalArgumentException("Ukjent behov: $it")
            }
        }
    }

    fun hentOpplysning(
        beskrivendeId: String,
        ident: String,
        søknadsId: String,
        behandlingsId: String,
    ): Opplysning {
        return repository.hent(
            beskrivendeId = beskrivendeId,
            ident = ident,
            søknadsId = UUID.fromString(søknadsId),
            behandlingsId = UUID.fromString(behandlingsId),
        )
    }

    fun publiserMeldingOmOpplysningBehovLøsning(
        ident: String,
        søknadsId: UUID,
        behandlingsId: UUID,
        løsning: Map<String, Any>,
    ) {
        rapid.publish(MeldingOmOpplysningBehovLøsning(ident, søknadsId, behandlingsId, løsning).asMessage().toJson())
    }
}
