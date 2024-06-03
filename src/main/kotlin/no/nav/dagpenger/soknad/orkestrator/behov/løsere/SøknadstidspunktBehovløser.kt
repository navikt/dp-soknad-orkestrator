package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Søknadstidspunkt
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class SøknadstidspunktBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: OpplysningRepository,
) : Behovløser(rapidsConnection, opplysningRepository) {
    override val behov = Søknadstidspunkt.name
    override val beskrivendeId = "søknadstidspunkt"

    override fun løs(behovmelding: Behovmelding) {
        val søknadstidspunkt =
            opplysningRepository.hent(beskrivendeId, behovmelding.ident, behovmelding.søknadId)?.svar ?: throw IllegalStateException(
                "Fant ingen opplysning med beskrivendeId: $beskrivendeId " +
                    "og kan ikke svare på behov $behov for søknad med id: ${behovmelding.søknadId}",
            )

        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val søknadsDato = ZonedDateTime.parse(søknadstidspunkt as String, formatter).toLocalDate()

        publiserLøsning(behovmelding, søknadsDato)
    }
}
