package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.AndreYtelser
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.helse.rapids_rivers.RapidsConnection

class AndreYtelserBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: OpplysningRepository,
) : Behovløser(rapidsConnection, opplysningRepository) {
    override val behov = AndreYtelser.name
    override val beskrivendeId = "faktum.andre-ytelser-mottatt-eller-sokt"
}
