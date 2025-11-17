package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.metrikker.SøknadMetrikker
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadPersonaliaRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.melding.MeldingOmSøknadInnsendt
import no.nav.dagpenger.soknad.orkestrator.søknad.melding.MeldingOmSøknadKlarTilJournalføring
import java.util.UUID

class SøknadService(
    private val søknadRepository: SøknadRepository,
    private val søknadPersonaliaRepository: SøknadPersonaliaRepository,
) {
    private lateinit var rapidsConnection: RapidsConnection

    fun setRapidsConnection(rapidsConnection: RapidsConnection) {
        this.rapidsConnection = rapidsConnection
    }

    fun søknadFinnes(søknadId: UUID) = søknadRepository.hent(søknadId) != null

    fun opprettOgLagreKomplettSøknaddata(
        ident: String,
        søknadId: UUID,
        seksjoner: JsonNode,
    ): ObjectNode {
        val komplettSøknaddata =
            objectMapper.createObjectNode().apply {
                put("ident", ident)
                put("søknadId", søknadId.toString())
                set<JsonNode>("seksjoner", seksjoner)
            }

        søknadRepository.lagreKomplettSøknadData(søknadId, komplettSøknaddata)
        return komplettSøknaddata
    }

    fun publiserMeldingOmSøknadInnsendt(
        søknadId: UUID,
        ident: String,
    ) {
        rapidsConnection.publish(ident, MeldingOmSøknadInnsendt(søknadId, ident).asMessage().toJson())
        SøknadMetrikker.varslet.inc()

        logg.info { "Publiserte melding om ny søknad med søknadId: $søknadId" }
        sikkerlogg.info { "Publiserte melding om ny søknad med søknadId: $søknadId og ident: $ident" }
    }

    internal fun slett(
        søknadId: UUID,
        ident: String,
    ) {
        val antallSøknaderSlettet = søknadRepository.slett(søknadId, ident)

        if (antallSøknaderSlettet > 0) {
            SøknadMetrikker.slettet.inc()
            logg.info { "Slettet søknad med søknadId: $søknadId" }
            sikkerlogg.info { "Slettet søknad med søknadId: $søknadId og ident: $ident" }
        }
    }

    fun opprett(ident: String): UUID {
        val søknadId = søknadRepository.lagre(Søknad(ident = ident))
        logg.info { "Opprettet søknad med søknadId $søknadId" }
        sikkerlogg.info { "Opprettet søknad med søknadId $søknadId for $ident" }
        return søknadId
    }

    fun sendInn(
        søknadId: UUID,
        ident: String,
    ) {
        logg.info { "Søknad $søknadId sendt inn" }
        sikkerlogg.info { "Søknad $søknadId sendt inn av $ident" }

        val melding = MeldingOmSøknadKlarTilJournalføring(søknadId, ident)

        rapidsConnection.publish(ident, melding.asMessage().toJson())
        SøknadMetrikker.mottatt.inc()

        logg.info { "Publiserte melding om søknad klar til journalføring med søknadId: $søknadId" }
        sikkerlogg.info { "Publiserte melding om søknad klar til journalføring med søknadId: $søknadId og ident: $ident" }
    }

    fun lagrePersonalia(søknadPersonalia: SøknadPersonalia) = søknadPersonaliaRepository.lagre(søknadPersonalia)

    private companion object {
        private val logg = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadService")
    }
}
