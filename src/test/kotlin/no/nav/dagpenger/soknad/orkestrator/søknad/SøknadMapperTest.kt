package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.BarnSvar
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
                    søknadId = søknadId,
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
    fun `kan mappe svar på flervalg faktum`() {
        val søknad = SøknadMapper(søknadsDataMedFlervalgFaktum).søknad
        val flervalgSvar = søknad.opplysninger.find { it.beskrivendeId == "faktum.eget-gaardsbruk-type-gaardsbruk" }?.svar

        flervalgSvar shouldBe listOf("svar.dyr", "svar.jord")
    }

    @Test
    fun `kan mappe svar på generatorfaktum - Arbeidsforhold`() {
        val søknad = SøknadMapper(søknadsDataMedGeneratorArbeidsforhold).søknad
        søknad.opplysninger.size shouldBe 2
        søknad.opplysninger.single { it.beskrivendeId == "faktum.arbeidsforhold" }.also { arbeidsforhold ->
            arbeidsforhold.beskrivendeId shouldBe "faktum.arbeidsforhold"
            arbeidsforhold.søknadId shouldBe søknadId
            arbeidsforhold.ident shouldBe ident
            arbeidsforhold.svar is Arbeidsforhold

            arbeidsforhold.svar.asListOf<ArbeidsforholdSvar>().let {
                it.size shouldBe 2

                it[0].navn shouldBe "Elektrikersjappa"
                it[0].land shouldBe "NOR"

                it[1].navn shouldBe "Bank AS"
                it[1].land shouldBe "NOR"
            }

            arbeidsforhold.svar.asListOf<ArbeidsforholdSvar>().let {
                it.size shouldBe 2

                it[0].navn shouldBe "Elektrikersjappa"
                it[0].land shouldBe "NOR"

                it[1].navn shouldBe "Bank AS"
                it[1].land shouldBe "NOR"
            }

            arbeidsforhold.svar.asListOf<ArbeidsforholdSvar>().let {
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
        val søknad = SøknadMapper(søknadsDataMedEøsArbeidsforhold).søknad
        søknad.opplysninger.size shouldBe 2
        søknad.opplysninger.single { it.beskrivendeId == "faktum.eos-arbeidsforhold" }.also { eøsArbeidsforhold ->
            eøsArbeidsforhold.beskrivendeId shouldBe "faktum.eos-arbeidsforhold"
            eøsArbeidsforhold.søknadId shouldBe søknadId
            eøsArbeidsforhold.ident shouldBe ident
            eøsArbeidsforhold.svar is EøsArbeidsforhold

            eøsArbeidsforhold.svar.asListOf<EøsArbeidsforholdSvar>().let {
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

    @Test
    fun `kan mappe svar på generatorfaktum - Egen næring`() {
        val søknad = SøknadMapper(søknadsDataMedEgenNæring).søknad
        søknad.opplysninger.single { it.beskrivendeId == "faktum.egen-naering-organisasjonsnummer-liste" }
            .also { opplysning ->
                opplysning.søknadId shouldBe søknadId
                opplysning.ident shouldBe ident
                opplysning.svar shouldBe listOf(123456789, 987654321)
            }
    }

    @Test
    fun `kan mappe svar på generatorfaktum - Register barn`() {
        val søknad = SøknadMapper(søknadsDataMedRegisterBarn).søknad
        søknad.opplysninger.single { it.beskrivendeId == "faktum.register.barn-liste" }.also { opplysning ->
            opplysning.søknadId shouldBe søknadId
            opplysning.ident shouldBe ident

            opplysning.svar.asListOf<BarnSvar>().also {
                it.size shouldBe 2

                it[0].fornavnOgMellomnavn shouldBe "Navn Register"
                it[0].etternavn shouldBe "Etternavn Register"
                it[0].fødselsdato shouldBe 1.januar(2024)
                it[0].statsborgerskap shouldBe "NOR"
                it[0].forsørgerBarnet shouldBe true
                it[1].fraRegister shouldBe true

                it[1].fornavnOgMellomnavn shouldBe "Navn Register 2"
                it[1].etternavn shouldBe "Etternavn Register 2"
                it[1].fødselsdato shouldBe 1.januar(2024)
                it[1].statsborgerskap shouldBe "NOR"
                it[1].forsørgerBarnet shouldBe false
                it[1].fraRegister shouldBe true
            }
        }
    }

    @Test
    fun `kan mappe svar på generatorfaktum - Barn`() {
        val søknad = SøknadMapper(søknadsDataMedBarn).søknad
        søknad.opplysninger.single { it.beskrivendeId == "faktum.barn-liste" }.also { opplysning ->
            opplysning.søknadId shouldBe søknadId
            opplysning.ident shouldBe ident

            opplysning.svar.asListOf<BarnSvar>().also {
                it.size shouldBe 2

                it[0].fornavnOgMellomnavn shouldBe "Navn"
                it[0].etternavn shouldBe "Etternavn"
                it[0].fødselsdato shouldBe 1.januar(2024)
                it[0].statsborgerskap shouldBe "NOR"
                it[0].forsørgerBarnet shouldBe true
                it[1].fraRegister shouldBe false

                it[1].fornavnOgMellomnavn shouldBe "Navn 2"
                it[1].etternavn shouldBe "Etternavn 2"
                it[1].fødselsdato shouldBe 1.januar(2024)
                it[1].statsborgerskap shouldBe "NOR"
                it[1].forsørgerBarnet shouldBe false
                it[1].fraRegister shouldBe false
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
          "@event_name": "søknad_innsendt_varsel",
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
          "@event_name": "søknad_innsendt_varsel",
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
          "@event_name": "søknad_innsendt_varsel",
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
          "@event_name": "søknad_innsendt_varsel",
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

private val søknadsDataMedFlervalgFaktum =
    ObjectMapper().readTree(
        //language=json
        """
        {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "@event_name": "søknad_innsendt_varsel",
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
                      "svar.dyr",
                      "svar.jord"
                    ],
                    "type": "flervalg",
                    "beskrivendeId": "faktum.eget-gaardsbruk-type-gaardsbruk"
                  }
                ]
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
          "@event_name": "søknad_innsendt_varsel",
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

private val søknadsDataMedEøsArbeidsforhold =
    ObjectMapper().readTree(
        //language=json
        """
        {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "@event_name": "søknad_innsendt_varsel",
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

private val søknadsDataMedEgenNæring =
    ObjectMapper().readTree(
        //language=json
        """
        {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "@event_name": "søknad_innsendt_varsel",
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
                          "svar": 123456789,
                          "type": "int",
                          "beskrivendeId": "faktum.egen-naering-organisasjonsnummer"
                        }
                      ],
                      [
                        {
                          "svar": 987654321,
                          "type": "int",
                          "beskrivendeId": "faktum.egen-naering-organisasjonsnummer"
                        }
                      ]
                    ],
                    "type": "generator",
                    "beskrivendeId": "faktum.egen-naering-organisasjonsnummer-liste"
                  }
                ]
              }
            ]
          }
        }
        """.trimIndent(),
    )

private val søknadsDataMedRegisterBarn =
    ObjectMapper().readTree(
        //language=json
        """
        {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "@event_name": "søknad_innsendt_varsel",
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
                          "svar": "Navn Register",
                          "type": "tekst",
                          "beskrivendeId": "faktum.barn-fornavn-mellomnavn"
                        },
                        {
                          "svar": "Etternavn Register",
                          "type": "tekst",
                          "beskrivendeId": "faktum.barn-etternavn"
                        },
                        {
                          "svar": "2024-01-01",
                          "type": "localdate",
                          "beskrivendeId": "faktum.barn-foedselsdato"
                        },
                        {
                          "svar": "NOR",
                          "type": "land",
                          "beskrivendeId": "faktum.barn-statsborgerskap"
                        },
                        {
                          "svar": true,
                          "type": "boolean",
                          "beskrivendeId": "faktum.forsoerger-du-barnet"
                        }
                      ],
                      [
                        {
                          "svar": "Navn Register 2",
                          "type": "tekst",
                          "beskrivendeId": "faktum.barn-fornavn-mellomnavn"
                        },
                        {
                          "svar": "Etternavn Register 2",
                          "type": "tekst",
                          "beskrivendeId": "faktum.barn-etternavn"
                        },
                        {
                          "svar": "2024-01-01",
                          "type": "localdate",
                          "beskrivendeId": "faktum.barn-foedselsdato"
                        },
                        {
                          "svar": "NOR",
                          "type": "land",
                          "beskrivendeId": "faktum.barn-statsborgerskap"
                        },
                        {
                          "svar": false,
                          "type": "boolean",
                          "beskrivendeId": "faktum.forsoerger-du-barnet"
                        }
                      ]
                    ],
                    "type": "generator",
                    "beskrivendeId": "faktum.register.barn-liste"
                  }
                ]
              }
            ]
          }
        }
        """.trimIndent(),
    )

private val søknadsDataMedBarn =
    ObjectMapper().readTree(
        //language=json
        """
        {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "@event_name": "søknad_innsendt_varsel",
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
                          "svar": "Navn",
                          "type": "tekst",
                          "beskrivendeId": "faktum.barn-fornavn-mellomnavn"
                        },
                        {
                          "svar": "Etternavn",
                          "type": "tekst",
                          "beskrivendeId": "faktum.barn-etternavn"
                        },
                        {
                          "svar": "2024-01-01",
                          "type": "localdate",
                          "beskrivendeId": "faktum.barn-foedselsdato"
                        },
                        {
                          "svar": "NOR",
                          "type": "land",
                          "beskrivendeId": "faktum.barn-statsborgerskap"
                        },
                        {
                          "svar": true,
                          "type": "boolean",
                          "beskrivendeId": "faktum.forsoerger-du-barnet"
                        }
                      ],
                      [
                        {
                          "svar": "Navn 2",
                          "type": "tekst",
                          "beskrivendeId": "faktum.barn-fornavn-mellomnavn"
                        },
                        {
                          "svar": "Etternavn 2",
                          "type": "tekst",
                          "beskrivendeId": "faktum.barn-etternavn"
                        },
                        {
                          "svar": "2024-01-01",
                          "type": "localdate",
                          "beskrivendeId": "faktum.barn-foedselsdato"
                        },
                        {
                          "svar": "NOR",
                          "type": "land",
                          "beskrivendeId": "faktum.barn-statsborgerskap"
                        },
                        {
                          "svar": false,
                          "type": "boolean",
                          "beskrivendeId": "faktum.forsoerger-du-barnet"
                        }
                      ]
                    ],
                    "type": "generator",
                    "beskrivendeId": "faktum.barn-liste"
                  }
                ]
              }
            ]
          }
        }
        """.trimIndent(),
    )
