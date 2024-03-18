package no.nav.dagpenger.soknad.orkestrator.behov

import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepositoryPostgres
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

abstract class Behovsløser(val rapidsConnection: RapidsConnection) {
    abstract val behov: String

    abstract fun løs(
        ident: String,
        søknadsId: UUID,
    )
}

class BehovLøserFactory(
    private val rapidsConnection: RapidsConnection,
    private val opplysningRepository: OpplysningRepositoryPostgres,
) {
    fun behovsløser(behov: String): Behovsløser {
        return when (behov) {
            "ØnskerDagpengerFraDato" ->
                ØnskerDagpengerFraDatoBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                )

            "EøsArbeid" ->
                EøsArbeidBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                )

            "KanJobbeDeltid" ->
                KanJobbeDeltidBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                )

            "HelseTilAlleTyperJobb" ->
                HelseTilAlleTyperJobbBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                )

            "KanJobbeHvorSomHelst" ->
                KanJobbeHvorSomHelstBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                )

            else -> throw IllegalArgumentException("Kan ikke løse behov: $behov")
        }
    }
}
