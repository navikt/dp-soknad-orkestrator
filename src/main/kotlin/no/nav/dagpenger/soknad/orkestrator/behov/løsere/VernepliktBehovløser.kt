package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.helse.rapids_rivers.RapidsConnection

class VernepliktBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: OpplysningRepository,
) : Behovløser(rapidsConnection, opplysningRepository) {
    override val behov = "Verneplikt"
    override val beskrivendeId = "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd"
}
