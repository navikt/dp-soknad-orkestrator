package no.nav.dagpenger.soknad.orkestrator.søknad.melding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import io.ktor.util.toLowerCasePreservingASCIIRules
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonMedTidstempler
import tools.jackson.databind.JsonNode
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
    fun asMessage(): JsonMessage =
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
            try {
                val aktuelleFelter = filtrerUaktuelleFelter()

                mapOf(
                    "innsendt" to (søknad.innsendtTidspunkt?.toString() ?: "null"),
                ) +
                    søknadsdata.associate {
                        if (it.data.isEmpty()) {
                            return@associate it.seksjonId to
                                mapOf("seksjonsdata" to null, "opprettet" to it.opprettet, "oppdatert" to it.oppdatert)
                        }

                        val aktuelleFelterForSeksjon =
                            aktuelleFelter
                                .find { pg ->
                                    pg.seksjonId == it.seksjonId.toLowerCasePreservingASCIIRules()
                                }?.spørsmål
                                ?: emptyList()
                        val seksjonsdataKlarTilStatistikk =
                            filtrerSeksjonsdataForStatistikk(it.seksjonId, it.data, aktuelleFelterForSeksjon)

                        it.seksjonId to
                            mapOf("seksjonsdata" to seksjonsdataKlarTilStatistikk, "opprettet" to it.opprettet, "oppdatert" to it.oppdatert)
                    }
            } catch (e: Exception) {
                return null
            }
        }

    fun filtrerSeksjonsdataForStatistikk(
        seksjonsId: String,
        seksjonsdata: String,
        gyldigeFeltIder: List<String> = emptyList(),
    ): String {
        val seksjonsdataJson = objectMapper.readTree(seksjonsdata)
        if (seksjonsId == "personalia") {
            return filtrerPersonaliaSeksjon(seksjonsdataJson)
        }

        val tillatteFelter = gyldigeFeltIder + ARRAY_ID_FELTER_SOM_MÅ_LEGGES_MANUELT
        val filtrertSeksjonsdata =
            filtrerUgyldigeSpørsmålBasertPåType(
                seksjonssvarJson = seksjonsdataJson["seksjonsvar"],
                tillatteFelter = tillatteFelter,
            )
        val seksjon =
            mapOf(
                "seksjonId" to seksjonsdataJson["seksjonId"].asString(),
                "seksjonsvar" to filtrertSeksjonsdata,
                "versjon" to seksjonsdataJson["versjon"].asInt(),
            )
        return objectMapper.writeValueAsString(seksjon)
    }

    private fun filtrerPersonaliaSeksjon(seksjonsdataJson: JsonNode): String {
        val filtrertPersonalia =
            mapOf(
                "folkeregistrertAdresseErNorgeStemmerDet" to
                    hentFeltFraSeksjon(seksjonsdataJson, "folkeregistrertAdresseErNorgeStemmerDet"),
                "bostedsland" to hentFeltFraSeksjon(seksjonsdataJson, "bostedsland"),
                "postnummerFraPdl" to hentFeltFraSeksjon(seksjonsdataJson, "postnummerFraPdl"),
                "landFraPdl" to hentFeltFraSeksjon(seksjonsdataJson, "landFraPdl"),
                "landkodeFraPdl" to hentFeltFraSeksjon(seksjonsdataJson, "landkodeFraPdl"),
            ).filterValues { it != "" }
        return objectMapper.writeValueAsString(
            mapOf(
                "seksjonId" to "personalia",
                "seksjonsvar" to filtrertPersonalia,
                "versjon" to seksjonsdataJson["versjon"].asInt(),
            ),
        )
    }

    fun filtrerUgyldigeSpørsmålBasertPåType(
        seksjonssvarJson: JsonNode,
        tillatteFelter: List<String>,
    ): Any? =
        when {
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
                seksjonssvarJson.values().map { element -> filtrerUgyldigeSpørsmålBasertPåType(element, tillatteFelter) }
            }

            seksjonssvarJson.isNull -> {
                null
            }

            else -> {
                seksjonssvarJson.asString()
            }
        }

    fun filtrerUaktuelleFelter(): List<SeksjonMedGyldigeFeltIder> {
        val gyldigeTyper =
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
                        if (node["type"].asString() in gyldigeTyper && node["id"].asString() != "fødselsdato") {
                            seksjonMappet.add(
                                node["id"].asString(),
                            )
                        }
                    }

                    node.isArray -> {
                        node.values().forEach { traverse(it) }
                    }

                    node.isObject -> {
                        node.values().forEach { traverse(it) }
                    }
                }
            }

            traverse(seksjon)
            SeksjonMedGyldigeFeltIder(
                seksjonId = hentSeksjonIdFraNavn(seksjon["navn"].asString()),
                spørsmål = seksjonMappet,
            )
        }
    }

    private fun hentSeksjonIdFraNavn(seksjonNavn: String): String =
        when (seksjonNavn) {
            "Reell arbeidssøker" -> "reell-arbeidssoker"
            "Egen næring" -> "egen-naring"
            "Annen pengestøtte" -> "annen-pengestotte"
            "Din situasjon" -> "din-situasjon"
            else -> seksjonNavn.toLowerCasePreservingASCIIRules()
        }

    private fun hentFeltFraSeksjon(
        jsonNode: JsonNode,
        nøkkel: String,
    ): String = jsonNode["seksjonsvar"][nøkkel]?.asString() ?: ""

    data class SeksjonMedGyldigeFeltIder(
        val seksjonId: String,
        val spørsmål: List<String> = emptyList(),
    )

    companion object {
        private val ARRAY_ID_FELTER_SOM_MÅ_LEGGES_MANUELT =
            listOf(
                "registrerteArbeidsforhold",
                "barnFraPdl",
                "barnLagtManuelt",
                "kanIkkeJobbeHeltidOgDeltidSituasjonenSomGjelderDeg",
                "kanIkkeJobbeIHeleNorgeSituasjonenSomGjelderDeg",
                "næringsvirksomheter",
                "gårdsbruk",
                "pengestøtteFraAndreEøsLand",
                "pengestøtteFraNorge",
            )
    }
}
