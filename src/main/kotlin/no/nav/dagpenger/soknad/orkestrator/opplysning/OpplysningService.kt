package no.nav.dagpenger.soknad.orkestrator.opplysning

import no.nav.dagpenger.soknad.orkestrator.behov.MeldingOmBehovLøsning
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
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
    ): Opplysning {
        return repository.hent(
            beskrivendeId = beskrivendeId,
            ident = ident,
            søknadsId = UUID.fromString(søknadsId),
        )
    }

    fun publiserMeldingOmOpplysningBehovLøsning(
        ident: String,
        søknadsId: UUID,
        løsning: Map<String, Any>,
    ) {
        rapid.publish(MeldingOmBehovLøsning(ident, søknadsId, løsning).asMessage().toJson())
    }
}
