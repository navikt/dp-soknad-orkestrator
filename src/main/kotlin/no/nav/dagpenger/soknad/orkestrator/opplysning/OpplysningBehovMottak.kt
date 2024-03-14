package no.nav.dagpenger.soknad.orkestrator.opplysning

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.UUID

class OpplysningBehovMottak(
    rapidsConnection: RapidsConnection,
    private val opplysningService: OpplysningService,
) : River.PacketListener {
    private val opplysningBehov =
        listOf(
            "Søknadstidspunkt",
            "JobbetUtenforNorge",
            "ØnskerDagpengerFraDato",
            "EøsArbeid",
            "KanJobbeDeltid",
            "HelseTilAlleTyperJobb",
            "KanJobbeHvorSomHelst",
            "VilligTilÅBytteYrke",
        )

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAllOrAny("@behov", opplysningBehov) }
            validate { it.requireKey("ident", "søknad_id", "behandling_id") }
            validate { it.rejectKey("@løsning") }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        with(packet) {
            val ident = get("ident").asText()
            val søknadsId = UUID.fromString(get("søknad_id").asText())
            val behandlingsId = UUID.fromString(get("behandling_id").asText())

            val behov: List<String> = get("@behov").map { it.asText() }.filter { it in opplysningBehov }
            val løsning = opplysningService.løsBehov(behov)

            // TODO: Publiser melding med løsning når løsBehvo er implementert
//            opplysningService.publiserMeldingOmOpplysningBehovLøsning(
//                ident = ident,
//                søknadsId = søknadsId,
//                behandlingsId = behandlingsId,
//                løsning = løsning,
//            )
        }
    }
}
