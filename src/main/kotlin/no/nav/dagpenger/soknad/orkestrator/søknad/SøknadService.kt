package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.api.models.OrkestratorSoknadDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SeksjonDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SeksjonsnavnDTO
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.metrikker.SøknadMetrikker
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningstype
import no.nav.dagpenger.soknad.orkestrator.opplysning.Svar
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.Bostedsland
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.Seksjon
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.getSeksjon
import no.nav.dagpenger.soknad.orkestrator.opplysning.toOpplysningDTO
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

    fun håndterSvar(
        søknadId: UUID,
        svar: Svar<*>,
    ) {
        val opplysning =
            opplysningRepository.hent(svar.opplysningId)
                ?: throw IllegalArgumentException("Fant ikke opplysning med id: ${svar.opplysningId}, kan ikke håndtere svar")

        val opplysningsbehovId = opplysning.opplysningsbehovId
        val seksjonsnavn = opplysning.seksjonsnavn
        val seksjon = getSeksjon(seksjonsnavn)

        try {
            seksjon.validerSvar(opplysningsbehovId, svar)
            opplysningRepository.lagreSvar(svar)
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Validering av svar feilet" }
            return
        }

        nullstillAvhengigheter(
            søknadId = søknadId,
            seksjon = seksjon,
            opplysningsbehovId = opplysningsbehovId,
        )

        håndterNesteOpplysning(søknadId, svar, seksjon, opplysningsbehovId)
    }

    private fun håndterNesteOpplysning(
        søknadId: UUID,
        svar: Svar<*>,
        seksjon: Seksjon,
        opplysningsbehovId: Int,
    ) {
        seksjon.nesteOpplysningsbehov(svar, opplysningsbehovId)?.let { nesteOpplysning ->
            val erLagretIDB =
                opplysningRepository
                    .hentAlleForSeksjon(søknadId, seksjon.navn)
                    .find { it.opplysningsbehovId == nesteOpplysning.id } != null

            if (!erLagretIDB) {
                val nyOpplysning =
                    Opplysning(
                        opplysningId = UUID.randomUUID(),
                        seksjonsnavn = seksjon.navn,
                        opplysningsbehovId = nesteOpplysning.id,
                        type = nesteOpplysning.type,
                        svar = null,
                    )

                opplysningRepository.lagre(søknadId, nyOpplysning)
            }
        }
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

    private fun toJson(svar: Svar<*>): String? =
        when (svar.type) {
            Opplysningstype.LAND, Opplysningstype.TEKST -> svar.verdi.toString()
            else -> objectMapper.writeValueAsString(svar.verdi)
        }

    private fun nullstillAvhengigheter(
        søknadId: UUID,
        seksjon: Seksjon,
        opplysningsbehovId: Int,
    ) {
        val avhengigheter = seksjon.avhengigheter(opplysningsbehovId)

        avhengigheter.forEach {
            opplysningRepository.slett(
                søknadId = søknadId,
                seksjonsnavn = seksjon.navn,
                opplysningsbehovId = it,
            )
        }
    }

    fun hentSøknad(søknadId: UUID): OrkestratorSoknadDTO {
        val alleOpplysninger = opplysningRepository.hentAlle(søknadId)
        val ubesvarteOpplysninger = alleOpplysninger.filter { it.svar == null }.sortedBy { it.opplysningsbehovId }
        val nesteUbesvarteOpplysning = ubesvarteOpplysninger.firstOrNull()

        val besvarteOpplysninger =
            if (nesteUbesvarteOpplysning == null) {
                alleOpplysninger
            } else {
                alleOpplysninger.filter { it.svar != null && it.opplysningsbehovId < nesteUbesvarteOpplysning.opplysningsbehovId }
            }
        val seksjon = getSeksjon(Bostedsland.navn) // TODO: Teit med default

        val nesteUbesvarteOpplysningDTO =
            nesteUbesvarteOpplysning?.let {
                seksjon.getOpplysningsbehov(it.opplysningsbehovId).toOpplysningDTO(it.opplysningId, null)
            }
        val besvarteOpplysningerDTO =
            besvarteOpplysninger.map {
                seksjon
                    .getOpplysningsbehov(it.opplysningsbehovId)
                    .toOpplysningDTO(it.opplysningId, toJson(it.svar!!))
            }

        val seksjoner =
            listOf(
                SeksjonDTO(
                    navn = SeksjonsnavnDTO.valueOf(seksjon.navn.name.lowercase()),
                    besvarteOpplysninger = besvarteOpplysningerDTO,
                    erFullført = nesteUbesvarteOpplysning == null,
                    nesteUbesvarteOpplysning = nesteUbesvarteOpplysningDTO,
                ),
            )

        val erSøknadFullført = seksjoner.all { it.erFullført }

        return OrkestratorSoknadDTO(
            søknadId = søknadId,
            seksjoner = seksjoner,
            antallSeksjoner = 1,
            erFullført = erSøknadFullført,
        )
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadService")
    }
}
