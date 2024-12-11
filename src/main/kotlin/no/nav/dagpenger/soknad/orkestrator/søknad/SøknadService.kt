package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.metrikker.SøknadMetrikker
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.Seksjon
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.getSeksjon
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import java.util.UUID

class SøknadService(
    private val søknadRepository: SøknadRepository,
    private val opplysningRepository: OpplysningRepository,
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

                val orkestratorOpplysninger = opplysningRepository.hentAlle(søknadId).groupBy { it.seksjonsnavn }

                val orkestratorSeksjoner =
                    orkestratorOpplysninger.map { (seksjonsnavn, opplysninger) ->
                        val seksjon = getSeksjon(seksjonsnavn)
                        val opplysningObjectNodes = opplysninger.toSøknadDataObjectNodes(seksjon)

                        objectMapper.createObjectNode().apply {
                            put("seksjonsnavn", seksjonsnavn.name)
                            set<ObjectNode>("opplysninger", objectMapper.valueToTree(opplysningObjectNodes))
                        }
                    }

                set<JsonNode>("orkestratorSeksjoner", objectMapper.valueToTree(orkestratorSeksjoner))
            }

        søknadRepository.lagreKomplettSøknadData(søknadId, komplettSøknaddata)
        return komplettSøknaddata
    }

    private fun List<Opplysning>.toSøknadDataObjectNodes(seksjon: Seksjon): List<ObjectNode> =
        this.map {
            val opplysningsbehov = seksjon.getOpplysningsbehov(it.opplysningsbehovId)

            val id = it.opplysningId
            val tekstnøkkel = opplysningsbehov.tekstnøkkel
            val type = opplysningsbehov.type
            val svar = it.svar!!.verdi
            val gyldigeSvar = opplysningsbehov.gyldigeSvar

            objectMapper.createObjectNode().apply {
                put("opplysningId", id.toString())
                put("tekstnøkkel", tekstnøkkel)
                put("type", type.name)
                set<JsonNode>("svar", objectMapper.valueToTree(svar))
                set<JsonNode>("gyldigeSvar", objectMapper.valueToTree(gyldigeSvar))
            }
        }

    fun publiserMeldingOmSøknadInnsendt(
        søknadId: UUID,
        ident: String,
    ) {
        rapidsConnection.publish(ident, MeldingOmSøknadInnsendt(søknadId, ident).asMessage().toJson())
        SøknadMetrikker.varslet.inc()

        logger.info { "Publiserte melding om ny søknad med søknadId: $søknadId" }
        sikkerlogg.info { "Publiserte melding om ny søknad med søknadId: $søknadId og ident: $ident" }
    }

    internal fun slett(
        søknadId: UUID,
        ident: String,
    ) {
        val antallSøknaderSlettet = søknadRepository.slett(søknadId)

        if (antallSøknaderSlettet > 0) {
            SøknadMetrikker.slettet.inc()
            logger.info { "Slettet søknad med søknadId: $søknadId" }
            sikkerlogg.info { "Slettet søknad med søknadId: $søknadId og ident: $ident" }
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadService")
    }
}
