package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.opplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

class LønnsgarantiBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: OpplysningRepository,
) : Behovløser(rapidsConnection, opplysningRepository) {
    override val behov = "Lønnsgaranti"
    override val beskrivendeId = "faktum.arbeidsforhold"

    override fun løs(packet: JsonMessage) {
        val svarPåBehov = rettTilDagpengerEtterKonkurs(packet.ident(), packet.søknadId())
        publiserLøsning(packet, svarPåBehov)
    }

    internal fun rettTilDagpengerEtterKonkurs(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val arbeidsforholdOpplysning = opplysningRepository.hent(beskrivendeId, ident, søknadId) ?: return false

        return arbeidsforholdOpplysning.svar.asListOf<ArbeidsforholdSvar>().any {
            it.sluttårsak == Sluttårsak.ARBEIDSGIVER_KONKURS
        }
    }
}
