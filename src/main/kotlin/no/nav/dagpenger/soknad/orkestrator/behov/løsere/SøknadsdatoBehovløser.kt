package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Søknadsdato
import no.nav.dagpenger.soknad.orkestrator.meldinger.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.ZonedDateTime

class SøknadsdatoBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: OpplysningRepository,
) : Behovløser(rapidsConnection, opplysningRepository) {
    override val behov = Søknadsdato.name
    override val beskrivendeId = "søknadstidspunkt"

    override fun løs(behovmelding: Behovmelding) {
        val opplysning =
            opplysningRepository.hent(beskrivendeId, behovmelding.ident, behovmelding.søknadId)
                ?: throw IllegalStateException(
                    "Fant ingen opplysning med beskrivendeId: $beskrivendeId " +
                        "og kan ikke svare på behov $behov for søknad med id: ${behovmelding.søknadId}",
                )

        val svarPåBehov = ZonedDateTime.parse(opplysning.svar as String).toLocalDate()
        publiserLøsning(behovmelding, svarPåBehov)
    }
}
