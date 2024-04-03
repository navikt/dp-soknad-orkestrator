package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Dato
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.EøsArbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.EøsArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.PeriodeSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.utils.februar
import no.nav.dagpenger.soknad.orkestrator.utils.januar
import no.nav.dagpenger.soknad.orkestrator.utils.mars
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID

private val søknadId = UUID.randomUUID()
private val ident = "12345678903"
private val søknadstidspunkt = ZonedDateTime.now().toString()
private val ønskerDagpengerFra = 1.januar

class SøknadMapperTest {
    @Test
    fun `vi kan mappe søknad_innsendt event riktig`() {
        val søknad = SøknadMapper(søknad_innsendt_event).søknad
        søknad.id shouldBe søknadId
        søknad.ident shouldBe ident
        søknad.opplysninger.size shouldBe 3

        søknad.opplysninger shouldContainAll
            listOf(
                Opplysning(
                    beskrivendeId = "faktum.mottatt-dagpenger-siste-12-mnd",
                    type = Tekst,
                    svar = "faktum.mottatt-dagpenger-siste-12-mnd.svar.nei",
                    ident = ident,
                    søknadsId = søknadId,
                ),
                Opplysning("faktum.dagpenger-soknadsdato", Dato, ønskerDagpengerFra, ident, søknadId),
                Opplysning("søknadstidspunkt", Tekst, søknadstidspunkt, ident, søknadId),
            )
    }

    @Test
    fun `vi kan ikke mappe dersom seksjoner mangler`() {
        shouldThrow<IllegalArgumentException> {
            SøknadMapper(søknadDataUtenSeksjoner).søknad
        }
    }

    @Test
    fun `vi kan ikke mappe dersom fakta mangler`() {
        shouldThrow<IllegalArgumentException> {
            SøknadMapper(søknaddataUtenFakta).søknad
        }
    }

    @Test
    fun `kan mappe svar på periodefaktum`() {
        val søknad = SøknadMapper(søknadsDataMedPeriodeFaktum).søknad
        val periodeSvar = søknad.opplysninger.find { it.beskrivendeId == "faktum.arbeidsforhold.varighet" }?.svar

        periodeSvar shouldBe PeriodeSvar(1.januar(2024), 1.februar(2024))
    }

    @Test
    fun `kan mappe svar på generatorfaktum - Arbeidsforhold`() {
        val søknad = SøknadMapper(søknadsDataMedGeneratorArbeidsforhold).søknad
        søknad.opplysninger.size shouldBe 2
        søknad.opplysninger.single { it.beskrivendeId == "faktum.arbeidsforhold" }.also { arbeidsforhold ->
            arbeidsforhold.beskrivendeId shouldBe "faktum.arbeidsforhold"
            arbeidsforhold.søknadsId shouldBe søknadId
            arbeidsforhold.ident shouldBe ident
            arbeidsforhold.svar is Arbeidsforhold

            (arbeidsforhold.svar as List<*>).filterIsInstance<ArbeidsforholdSvar>().let {
                it.size shouldBe 2

                it[0].navn shouldBe "Elektrikersjappa"
                it[0].land shouldBe "NOR"

                it[1].navn shouldBe "Bank AS"
                it[1].land shouldBe "NOR"
            }
        }
    }

    @Test
    fun `kan mappe svar på generatorfaktum - Eøs Arbeidsforhold`() {
        val søknad = SøknadMapper(søknadsDataMedGeneratorEøsArbeidsforhold).søknad
        søknad.opplysninger.size shouldBe 2
        søknad.opplysninger.single { it.beskrivendeId == "faktum.eos-arbeidsforhold" }.also { eøsArbeidsforhold ->
            eøsArbeidsforhold.beskrivendeId shouldBe "faktum.eos-arbeidsforhold"
            eøsArbeidsforhold.søknadsId shouldBe søknadId
            eøsArbeidsforhold.ident shouldBe ident
            eøsArbeidsforhold.svar is EøsArbeidsforhold

            (eøsArbeidsforhold.svar as List<*>).filterIsInstance<EøsArbeidsforholdSvar>().let {
                it.size shouldBe 2

                it[0].bedriftnavn shouldBe "Utlandet AS"
                it[0].land shouldBe "NLD"
                it[0].personnummerIArbeidsland shouldBe "123567890"
                it[0].varighet shouldBe PeriodeSvar(11.mars(2024), 24.mars(2024))

                it[1].bedriftnavn shouldBe "Utlandet 2 AS"
                it[1].land shouldBe "FRA"
                it[1].personnummerIArbeidsland shouldBe "23456789"
                it[1].varighet shouldBe PeriodeSvar(6.februar(2024), null)
            }
        }
    }
}

private val søknad_innsendt_event =
    //language=json
    ObjectMapper().readTree(
        """
        {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "@event_name": "søknad_innsendt",
          "@opprettet": "2024-02-21T11:00:27.899791748",
          "søknadId": "$søknadId",
          "ident": "$ident",
          "søknadstidspunkt": "$søknadstidspunkt",
          "søknadData": {
            "søknad_uuid": "$søknadId",
            "@opprettet": "2024-02-21T11:00:27.899791748",
            "seksjoner": [
              {
                "fakta": [
                  {
                    "svar": "faktum.mottatt-dagpenger-siste-12-mnd.svar.nei",
                    "type": "envalg",
                    "beskrivendeId": "faktum.mottatt-dagpenger-siste-12-mnd"
                  },
                  {
                    "svar": "$ønskerDagpengerFra",
                    "type": "localdate",
                    "beskrivendeId": "faktum.dagpenger-soknadsdato"
                  }
                ],
                "beskrivendeId": "din-situasjon"
              }
            ]
          }
        }
        """.trimIndent(),
    )

private val søknadDataUtenSeksjoner =
    ObjectMapper().readTree(
        //language=json
        """
        {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "@event_name": "søknad_innsendt",
          "@opprettet": "2024-02-21T11:00:27.899791748",
          "søknadId": "$søknadId",
          "ident": "$ident",
          "søknadstidspunkt": "$søknadstidspunkt",
          "søknadData": {
            "søknad_uuid": "$søknadId",
            "@opprettet": "2024-02-21T11:00:27.899791748"
          }
        }
        """.trimIndent(),
    )

private val søknaddataUtenFakta =
    ObjectMapper().readTree(
        //language=json
        """
        {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "@event_name": "søknad_innsendt",
          "@opprettet": "2024-02-21T11:00:27.899791748",
          "søknadId": "$søknadId",
          "ident": "$ident",
          "søknadstidspunkt": "$søknadstidspunkt",
          "søknadData": {
            "søknad_uuid": "$søknadId",
            "@opprettet": "2024-02-21T11:00:27.899791748",
            "seksjoner": [
              {
                "beskrivendeId": "bostedsland"
              }
            ]
          }
        }
        """.trimIndent(),
    )

private val søknadsDataMedPeriodeFaktum =
    ObjectMapper().readTree(
        //language=json
        """
        {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "@event_name": "søknad_innsendt",
          "@opprettet": "2024-02-21T11:00:27.899791748",
          "søknadId": "$søknadId",
          "ident": "$ident",
          "søknadstidspunkt": "$søknadstidspunkt",
          "søknadData": {
            "søknad_uuid": "$søknadId",
            "@opprettet": "2024-02-21T11:00:27.899791748",
            "seksjoner": [
              {
                "fakta": [
                  {
                    "id": "6001",
                    "svar": {
                      "fom": "2024-01-01",
                      "tom": "2024-02-01"
                    },
                    "type": "periode",
                    "beskrivendeId": "faktum.arbeidsforhold.varighet"
                  }
                ],
                "beskrivendeId": "din-situasjon"
              }
            ]
          }
        }
        """.trimIndent(),
    )

private val søknadsDataMedGeneratorArbeidsforhold =
    ObjectMapper().readTree(
        //language=json
        """
        {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "@event_name": "søknad_innsendt",
          "@opprettet": "2024-02-21T11:00:27.899791748",
          "søknadId": "$søknadId",
          "ident": "$ident",
          "søknadstidspunkt": "$søknadstidspunkt",
          "søknadData": {
            "søknad_uuid": "$søknadId",
            "@opprettet": "2024-02-21T11:00:27.899791748",
            "seksjoner": [
              {
                "fakta": [
                  {
                    "svar": [
                      [
                        {
                          "svar": "Elektrikersjappa",
                          "type": "tekst",
                          "beskrivendeId": "faktum.arbeidsforhold.navn-bedrift"
                        },
                        {
                          "svar": "NOR",
                          "type": "land",
                          "beskrivendeId": "faktum.arbeidsforhold.land"
                        },
                        {
                          "svar": "faktum.arbeidsforhold.endret.svar.sagt-opp-av-arbeidsgiver",
                          "type": "envalg",
                          "beskrivendeId": "faktum.arbeidsforhold.endret"
                        },
                        {
                          "svar": {
                            "fom": "2024-01-01",
                            "tom": "2024-03-31"
                          },
                          "type": "periode",
                          "beskrivendeId": "faktum.arbeidsforhold.varighet"
                        },
                        {
                          "svar": true,
                          "beskrivendeId": "faktum.arbeidsforhold.vet-du-antall-timer-foer-mistet-jobb"
                        },
                        {
                          "svar": 37.0,
                          "type": "double",
                          "beskrivendeId": "faktum.arbeidsforhold.antall-timer-dette-arbeidsforhold"
                        },
                        {
                          "svar": "Fordi",
                          "type": "tekst",
                          "beskrivendeId": "faktum.arbeidsforhold.vet-du-aarsak-til-sagt-opp-av-arbeidsgiver"
                        },
                        {
                          "svar": false,
                          "type": "boolean",
                          "beskrivendeId": "faktum.arbeidsforhold.tilbud-om-annen-stilling-eller-annet-sted-i-norge"
                        },
                        {
                          "svar": false,
                          "type": "boolean",
                          "beskrivendeId": "faktum.arbeidsforhold.skift-eller-turnus"
                        },
                        {
                          "svar": false,
                          "type": "boolean",
                          "beskrivendeId": "faktum.arbeidsforhold.rotasjon"
                        }
                      ],
                      [
                        {
                          "svar": "Bank AS",
                          "type": "tekst",
                          "beskrivendeId": "faktum.arbeidsforhold.navn-bedrift"
                        },
                        {
                          "svar": "NOR",
                          "type": "land",
                          "beskrivendeId": "faktum.arbeidsforhold.land"
                        },
                        {
                          "svar": "faktum.arbeidsforhold.endret.svar.kontrakt-utgaatt",
                          "type": "envalg",
                          "beskrivendeId": "faktum.arbeidsforhold.endret"
                        },
                        {
                          "svar": {
                            "fom": "2023-01-01",
                            "tom": "2024-03-31"
                          },
                          "type": "periode",
                          "beskrivendeId": "faktum.arbeidsforhold.varighet"
                        },
                        {
                          "svar": false,
                          "type": "boolean",
                          "beskrivendeId": "faktum.arbeidsforhold.vet-du-antall-timer-foer-kontrakt-utgikk"
                        },
                        {
                          "svar": false,
                          "type": "boolean",
                          "beskrivendeId": "faktum.arbeidsforhold.tilbud-om-forlengelse-eller-annen-stilling"
                        },
                        {
                          "svar": false,
                          "type": "boolean",
                          "beskrivendeId": "faktum.arbeidsforhold.skift-eller-turnus"
                        },
                        {
                          "svar": false,
                          "type": "boolean",
                          "beskrivendeId": "faktum.arbeidsforhold.rotasjon"
                        }
                      ]
                    ],
                    "type": "generator",
                    "beskrivendeId": "faktum.arbeidsforhold"
                  }
                ],
                "beskrivendeId": "din-situasjon"
              }
            ]
          }
        }
        """.trimIndent(),
    )

private val søknadsDataMedGeneratorEøsArbeidsforhold =
    ObjectMapper().readTree(
        //language=json
        """
        {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "@event_name": "søknad_innsendt",
          "@opprettet": "2024-02-21T11:00:27.899791748",
          "søknadId": "$søknadId",
          "ident": "$ident",
          "søknadstidspunkt": "$søknadstidspunkt",
          "søknadData": {
            "søknad_uuid": "$søknadId",
            "@opprettet": "2024-02-21T11:00:27.899791748",
            "seksjoner": [
              {
                "fakta": [
                  {
                    "svar": [
                      [
                        {
                          "svar": "Utlandet AS",
                          "type": "tekst",
                          "beskrivendeId": "faktum.eos-arbeidsforhold.arbeidsgivernavn"
                        },
                        {
                          "svar": "NLD",
                          "type": "land",
                          "beskrivendeId": "faktum.eos-arbeidsforhold.land"
                        },
                        {
                          "svar": "123567890",
                          "type": "tekst",
                          "beskrivendeId": "faktum.eos-arbeidsforhold.personnummer"
                        },
                        {
                          "svar": {
                            "fom": "2024-03-11",
                            "tom": "2024-03-24"
                          },
                          "type": "periode",
                          "beskrivendeId": "faktum.eos-arbeidsforhold.varighet"
                        }
                      ],
                      [
                        {
                          "svar": "Utlandet 2 AS",
                          "type": "tekst",
                          "beskrivendeId": "faktum.eos-arbeidsforhold.arbeidsgivernavn"
                        },
                        {
                          "svar": "FRA",
                          "type": "land",
                          "beskrivendeId": "faktum.eos-arbeidsforhold.land"
                        },
                        {
                          "svar": "23456789",
                          "type": "tekst",
                          "beskrivendeId": "faktum.eos-arbeidsforhold.personnummer"
                        },
                        {
                          "svar": {
                            "fom": "2024-02-06"
                          },
                          "type": "periode",
                          "beskrivendeId": "faktum.eos-arbeidsforhold.varighet"
                        }
                      ]
                    ],
                    "type": "generator",
                    "beskrivendeId": "faktum.eos-arbeidsforhold"
                  }
                ],
                "beskrivendeId": "eos-arbeidsforhold"
              }
            ]
          }
        }
        """.trimIndent(),
    )
