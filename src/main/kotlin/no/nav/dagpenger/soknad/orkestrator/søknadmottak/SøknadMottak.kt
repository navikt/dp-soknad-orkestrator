package no.nav.dagpenger.soknad.orkestrator.søknadmottak

import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.Søknad
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.objectMapper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class SøknadMottak(rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "innsending_ferdigstilt") }
            validate { it.demandAny("type", listOf("NySøknad")) }
            validate { it.requireKey("fødselsnummer") }
            validate { it.requireKey("søknadsData") }
            validate { it.interestedIn("@id", "@opprettet") }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val legacySøknad = packet.toLegacySøknad()
        val opplysninger =
            packet.toLegacySøknad().søknadsData.seksjoner.map { seksjon ->
                seksjon.fakta.map { fakta ->
                    Opplysning(fakta.svar, fakta.beskrivendeId)
                }
            }.toList().flatten()

        Søknad(
            id = UUID.fromString(legacySøknad.søknadsData.søknad_uuid),
            fødselsnummer = legacySøknad.fødselsnummer,
            journalpostId = legacySøknad.journalpostId,
            // TODO: Finne nøyaktig søknadstidspunkt
            søknadstidspunkt = legacySøknad.opprettet.toLocalDateTime(),
            opplysninger = opplysninger,
        )
    }
}

fun String.toLocalDateTime(): LocalDateTime {
    return LocalDateTime.parse(this, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS"))
}

fun JsonMessage.toLegacySøknad(): LegacySøknad {
    return objectMapper.readValue(this.toJson(), LegacySøknad::class.java)
}
