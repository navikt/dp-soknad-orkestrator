package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.meldinger.BehovMelding
import no.nav.dagpenger.soknad.orkestrator.opplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

class OrdinærBehovløser(rapidsConnection: RapidsConnection, opplysningRepository: OpplysningRepository) :
    Behovløser(rapidsConnection, opplysningRepository) {
    override val behov = BehovløserFactory.Behov.Ordinær.name
    override val beskrivendeId = "faktum.arbeidsforhold"

    override fun løs(behovMelding: BehovMelding) {
        val svarPåBehov = rettTilOrdinæreDagpenger(behovMelding.ident, behovMelding.søknadId)
        publiserLøsning(behovMelding, svarPåBehov)
    }

    internal fun rettTilOrdinæreDagpenger(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val arbeidsforholdOpplysning = opplysningRepository.hent(beskrivendeId, ident, søknadId) ?: return false

        val ikkeOrdinæreRettighetstyper =
            setOf(Sluttårsak.PERMITTERT, Sluttårsak.PERMITTERT_FISKEFOREDLING, Sluttårsak.ARBEIDSGIVER_KONKURS)

        return arbeidsforholdOpplysning.svar.asListOf<ArbeidsforholdSvar>().none {
            it.sluttårsak in ikkeOrdinæreRettighetstyper
        }
    }
}
