package no.nav.dagpenger.soknad.orkestrator.søknad.melding

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import io.ktor.util.toLowerCasePreservingASCIIRules
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonMedTidstempler
import java.time.LocalDateTime
import java.util.UUID

class SøknadEndretTilstandMelding(
    private val søknadId: UUID,
    private val ident: String,
    private val forrigeTilstand: String,
    private val nyTilstand: String,
    private val søknadsdata: List<SeksjonMedTidstempler> = emptyList(),
    private val pdfGrunnlag: List<String> = emptyList(),
    private val søknad: Søknad? = null,
) {
    @Suppress("UNCHECKED_CAST")
    fun asMessage(): JsonMessage {
        var response =
            JsonMessage.newMessage(
                eventName = "søknad_endret_tilstand",
                map =
                    mapOf(
                        "søknad_uuid" to søknadId,
                        "ident" to ident,
                        "forrigeTilstand" to mapTilstandsNavn(forrigeTilstand),
                        "gjeldendeTilstand" to mapTilstandsNavn(nyTilstand),
                        "kilde" to "orkestrator",
                        "søknadsdata" to lagSøknadsdataForStatistikk(),
                    ).filterValues { it != null } as Map<String, Any>,
            )

        return response
    }

    private fun mapTilstandsNavn(tilstand: String): String =
        when (tilstand) {
            "PÅBEGYNT" -> "Påbegynt"
            "INNSENDT" -> "Innsendt"
            "SLETTET_AV_SYSTEM" -> "Slettet"
            else -> tilstand
        }

    private fun lagSøknadsdataForStatistikk(): Map<String, Any>? =
        if (søknadsdata.isEmpty() || søknad == null) {
            null
        } else {
            val gyldigeFelter = gyldigeFelterForStatistikk()

            mapOf(
                "opprettet" to LocalDateTime.now(),
                "innsendt" to (søknad.innsendtTidspunkt?.toString() ?: "null"),
            ) +
                søknadsdata.associate {
                    if (it.data.isEmpty()) {
                        return@associate it.seksjonId to
                            mapOf("seksjonsdata" to null, "opprettet" to it.opprettet, "oppdatert" to it.oppdatert)
                    }
                    val gyldigeFelterForSeksjon =
                        gyldigeFelter
                            .find { pg ->
                                pg.seksjonId.replace(" ", "-").toLowerCasePreservingASCIIRules() ==
                                    it.seksjonId.toLowerCasePreservingASCIIRules()
                            }?.spørsmål
                            ?: emptyList()
                    val seksjonsdataKlarTilStatistikk = filtrerSeksjonsdataForStatistikk(it.seksjonId, it.data, gyldigeFelterForSeksjon)

                    it.seksjonId to
                        mapOf("seksjonsdata" to seksjonsdataKlarTilStatistikk, "opprettet" to it.opprettet, "oppdatert" to it.oppdatert)
                }
        }

    fun filtrerSeksjonsdataForStatistikk(
        seksjonsId: String,
        seksjonsdata: String,
        gyldigeFeltIder: List<String> = emptyList(),
    ): String {
        val seksjonsdataJson = objectMapper.readTree(seksjonsdata)
        if (seksjonsId == "personalia") {
            val filtrertPersonalia =
                mapOf(
                    "folkeregistrertAdresseErNorgeStemmerDet" to
                        hentFeltFraSeksjon(seksjonsdataJson, "folkeregistrertAdresseErNorgeStemmerDet"),
                    "bostedsland" to hentFeltFraSeksjon(seksjonsdataJson, "bostedsland"),
                ).filterValues { it != "" }
            return objectMapper.writeValueAsString(filtrertPersonalia)
        }

        val tillatteFelter = gyldigeFeltIder + listOf("registrerteArbeidsforhold")
        val filtrertSeksjonsdata =
            filtrerUgyldigeSpørsmålBasertPåType(
                seksjonssvarJson = seksjonsdataJson["seksjonsvar"],
                tillatteFelter = tillatteFelter,
            )
        val seksjon =
            mapOf(
                "seksjonId" to seksjonsdataJson["seksjonId"].asText(),
                "seksjonsvar" to filtrertSeksjonsdata,
                "versjon" to seksjonsdataJson["versjon"].asInt(),
            )
        return objectMapper.writeValueAsString(seksjon)
    }

    fun filtrerUgyldigeSpørsmålBasertPåType(
        seksjonssvarJson: JsonNode,
        tillatteFelter: List<String>,
    ): Any? {
        println("seksjonssvarJson: $seksjonssvarJson")
        return when {
            seksjonssvarJson.isObject -> {
                val result = mutableMapOf<String, Any?>()
                seksjonssvarJson.properties().forEach { (key, value) ->
                    if (key in tillatteFelter) {
                        result[key] = filtrerUgyldigeSpørsmålBasertPåType(value, tillatteFelter)
                    }
                }
                result
            }

            seksjonssvarJson.isArray -> {
                seksjonssvarJson.map { element -> filtrerUgyldigeSpørsmålBasertPåType(element, tillatteFelter) }
            }

            seksjonssvarJson.isNull -> {
                null
            }

            else -> {
                seksjonssvarJson.asText()
            }
        }
    }

    fun gyldigeFelterForStatistikk(): List<SeksjonMedGyldigeFeltIder> {
        val listOverGyldigeTyper =
            listOf(
                "envalg",
                "flervalg",
                "dato",
                "periodeFra",
                "periodeTil",
                "land",
                "tall",
                "nedtrekksliste",
            )

        return pdfGrunnlag.map {
            val seksjon = objectMapper.readTree(it)
            val seksjonMappet = mutableListOf<String>()

            fun traverse(node: JsonNode) {
                when {
                    node.isObject && node.has("id") && node.has("type") -> {
                        if (node["type"].asText() in listOverGyldigeTyper) {
                            seksjonMappet.add(
                                node["id"].asText(),
                            )
                        }
                    }

                    node.isArray -> {
                        node.forEach { traverse(it) }
                    }

                    node.isObject -> {
                        node.values().forEach { traverse(it) }
                    }
                }
            }

            traverse(seksjon)
            SeksjonMedGyldigeFeltIder(
                seksjonId = seksjon["navn"].asText(),
                spørsmål = seksjonMappet,
            )
        }
    }

    private fun hentFeltFraSeksjon(
        jsonNode: JsonNode,
        nøkkel: String,
    ): String = jsonNode["seksjonsvar"][nøkkel]?.asText() ?: ""

    data class SeksjonMedGyldigeFeltIder(
        val seksjonId: String,
        val spørsmål: List<String> = emptyList(),
    )
}
