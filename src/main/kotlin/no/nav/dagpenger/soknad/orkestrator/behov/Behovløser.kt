package no.nav.dagpenger.soknad.orkestrator.behov

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.metrikker.BehovMetrikker
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

abstract class Behovløser(
    val rapidsConnection: RapidsConnection,
    val opplysningRepository: QuizOpplysningRepository,
    val seksjonRepository: SeksjonRepository? = null,
) {
    abstract val behov: String
    abstract val beskrivendeId: String

    internal open fun løs(behovmelding: Behovmelding) {
        val svarPåBehov =
            opplysningRepository.hent(beskrivendeId, behovmelding.ident, behovmelding.søknadId)?.svar
                ?: throw IllegalStateException(
                    "Fant ingen opplysning med beskrivendeId: $beskrivendeId " +
                        "og kan ikke svare på behov $behov for søknad med id: ${behovmelding.søknadId}",
                )

        publiserLøsning(behovmelding, svarPåBehov)
    }

    internal fun publiserLøsning(
        behovmelding: Behovmelding,
        svarPåBehov: Any,
    ) {
        leggLøsningPåBehovmelding(behovmelding, svarPåBehov)
        rapidsConnection.publish(behovmelding.ident, behovmelding.innkommendePacket.toJson())

        BehovMetrikker.løst.labelValues(behov).inc()
        logger.info { "Løste behov $behov" }
        sikkerlogg.info { "Løste behov $behov med løsning: $svarPåBehov" }
    }

    private fun leggLøsningPåBehovmelding(
        behovmelding: Behovmelding,
        svarPåBehov: Any,
    ) {
        var gjelderFra: LocalDate? = finnGjelderFraDato(behovmelding.søknadId, behovmelding.ident)

        // TODO: Dette funker ikke ordentlig
        //  Det er litt rart å bruke søknadRepository via seksjonRepository, vi burde heller sende den inn direkte
        //  innsendtTidspunkt er litt rar, så jeg tror ikke vi nødvendigvis får riktig dato
        if (gjelderFra == null) {
            val søknad =
                seksjonRepository
                    ?.søknadRepository
                    ?.hent(behovmelding.søknadId)

            gjelderFra =
                søknad
                    ?.innsendtTidspunkt
                    ?.toLocalDate()
                    ?: throw IllegalStateException(
                        "Fant ingen opplysning om innsendt dato med beskrivendeId " +
                            "og kan ikke svare på behov $behov for søknad med id: ${behovmelding.søknadId}",
                    )
        }
        behovmelding.innkommendePacket["@løsning"] =
            mapOf(
                behov to
                    mapOf(
                        "verdi" to svarPåBehov,
                        "gjelderFra" to gjelderFra,
                    ),
            )
    }

    internal companion object {
        val logger = KotlinLogging.logger {}
        val sikkerlogg = KotlinLogging.logger("tjenestekall.Behovløser")
    }

    fun JsonMessage.søknadId(): UUID = UUID.fromString(get("søknadId").asText())

    fun JsonMessage.ident(): String = get("ident").asText()

    // I første omgang er gjelderFra lik søknadstidspunkt
    fun finnGjelderFraDato(
        søknadId: UUID,
        ident: String,
    ): LocalDate? {
        val søknadstidspunkt =
            opplysningRepository
                .hent(
                    beskrivendeId = "søknadstidspunkt",
                    ident = ident,
                    søknadId = søknadId,
                )?.svar

        return søknadstidspunkt?.let {
            ZonedDateTime.parse(it as String, DateTimeFormatter.ISO_ZONED_DATE_TIME).toLocalDate()
        }
    }
}
