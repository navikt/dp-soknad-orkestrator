package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.EØSArbeid
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

class EøsArbeidBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: OpplysningRepository,
) : Behovløser(rapidsConnection, opplysningRepository) {
    override val behov = EØSArbeid.name
    override val beskrivendeId = "faktum.eos-arbeid-siste-36-mnd"

    override fun løs(behovmelding: Behovmelding) {
        val svarPåBehov = harJobbetIEøsSiste36mnd(behovmelding.ident, behovmelding.søknadId)
        publiserLøsning(behovmelding, svarPåBehov)
    }

    internal fun harJobbetIEøsSiste36mnd(
        ident: String,
        søknadId: UUID,
    ) =
        // Dersom vi ikke finner opplysningen betyr det at bruker ikke har besvart dette spørsmålet fordi hen ikke har vært i arbeid. Vi svarer da false.
        opplysningRepository.hent(beskrivendeId, ident, søknadId)?.svar ?: false
}
