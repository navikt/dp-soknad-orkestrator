package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.meldinger.BehovMelding
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

class EøsArbeidBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: OpplysningRepository,
) : Behovløser(rapidsConnection, opplysningRepository) {
    override val behov = "EøsArbeid"
    override val beskrivendeId = "faktum.eos-arbeid-siste-36-mnd"

    override fun løs(behovMelding: BehovMelding) {
        val svarPåBehov = harJobbetIEøsSiste36mnd(behovMelding.ident, behovMelding.søknadId)
        publiserLøsning(behovMelding, svarPåBehov)
    }

    internal fun harJobbetIEøsSiste36mnd(
        ident: String,
        søknadId: UUID,
    ) =
        // Dersom vi ikke finner opplysningen betyr det at bruker ikke har besvart dette spørsmålet fordi hen ikke har vært i arbeid. Vi svarer da false.
        opplysningRepository.hent(beskrivendeId, ident, søknadId)?.svar ?: false
}
