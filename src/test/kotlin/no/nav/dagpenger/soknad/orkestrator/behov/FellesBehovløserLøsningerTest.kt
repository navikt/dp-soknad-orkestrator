package no.nav.dagpenger.soknad.orkestrator.behov

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Boolsk
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Dato
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FellesBehovløserLøsningerTest {
    val opplysningRepository = mockk<QuizOpplysningRepository>(relaxed = true)
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    val fellesBehovLøserLøsninger =
        FellesBehovløserLøsninger(opplysningRepository, søknadRepository, seksjonRepository)

    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `Søkeren har hatt arbeidsforhold i EØS de siste 36 månedene med quiz søknad`() {
        val cases =
            listOf(
                Pair(true, true),
                Pair(false, false),
            )
        cases.forEach { (arbeidsforholdIEøs, forventedSvar) ->

            val opplysning =
                QuizOpplysning(
                    beskrivendeId = "faktum.eos-arbeid-siste-36-mnd",
                    type = Boolsk,
                    svar = arbeidsforholdIEøs,
                    ident = ident,
                    søknadId = søknadId,
                )
            every {
                opplysningRepository.hent(
                    any(),
                    any(),
                    any(),
                )
            }.returns(opplysning)

            val result =
                fellesBehovLøserLøsninger.harSøkerenHattArbeidsforholdIEøs(
                    beskrivendeId = "faktum.eos-arbeid-siste-36-mnd",
                    ident = ident,
                    søknadId = søknadId,
                )

            assertTrue(result == forventedSvar)

            verify {
                opplysningRepository.hent(
                    beskrivendeId = "faktum.eos-arbeid-siste-36-mnd",
                    ident = ident,
                    søknadId = søknadId,
                )
            }

            verify(exactly = 0) {
                seksjonRepository.hentSeksjonsvarEllerKastException(
                    ident = ident,
                    søknadId = søknadId,
                    seksjonId = "arbeidsforhold",
                )
            }
        }
    }

    @Test
    fun `Søkeren har hatt arbeidsforhold i EØS de siste 36 månedene med orkestrator søknad`() {
        val cases =
            listOf(
                Pair(true, true),
                Pair(false, false),
            )
        cases.forEach { (arbeidsforholdIEøs, forventedSvar) ->
            val arbeidsforholdSvar = if (arbeidsforholdIEøs) "ja" else "nei"

            every {
                opplysningRepository.hent(
                    any(),
                    any(),
                    any(),
                )
            } returns null
            every {
                seksjonRepository.hentSeksjonsvarEllerKastException(
                    any(),
                    any(),
                    any(),
                )
            } returns
                objectMapper.readTree(
                    """
{
  
    "harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36Månedene": "$arbeidsforholdSvar"
  
}
                    """.trimIndent(),
                )

            val result =
                fellesBehovLøserLøsninger.harSøkerenHattArbeidsforholdIEøs(
                    beskrivendeId = "faktum.eos-arbeid-siste-36-mnd",
                    ident = ident,
                    søknadId = søknadId,
                )

            assertTrue(result == forventedSvar)

            verify {
                opplysningRepository.hent(
                    beskrivendeId = "faktum.eos-arbeid-siste-36-mnd",
                    ident = ident,
                    søknadId = søknadId,
                )
            }
            verify {
                seksjonRepository.hentSeksjonsvarEllerKastException(
                    ident = ident,
                    søknadId = søknadId,
                    seksjonId = "arbeidsforhold",
                )
            }
        }
    }

    @Test
    fun `Søkeren har hatt arbeidsforhold i EØS de siste 36 månedene med orkestrator søknad med ubesvart spørsmål`() {
        every {
            opplysningRepository.hent(
                any(),
                any(),
                any(),
            )
        } returns null
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                any(),
                any(),
                any(),
            )
        } returns
            objectMapper.readTree(
                """
                {
                    "harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritannia": "nei"
                }
                """.trimIndent(),
            )

        val result =
            fellesBehovLøserLøsninger.harSøkerenHattArbeidsforholdIEøs(
                beskrivendeId = "faktum.eos-arbeid-siste-36-mnd",
                ident = ident,
                søknadId = søknadId,
            )

        assertFalse { result }

        verify {
            opplysningRepository.hent(
                beskrivendeId = "faktum.eos-arbeid-siste-36-mnd",
                ident = ident,
                søknadId = søknadId,
            )
        }
        verify {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident = ident,
                søknadId = søknadId,
                seksjonId = "arbeidsforhold",
            )
        }
    }

    @Test
    fun `Søkeren har hatt arbeidsforhold i EØS de siste 36 månedene blir false om info ikke finnes`() {
        every {
            opplysningRepository.hent(
                any(),
                any(),
                any(),
            )
        } returns null
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                any(),
                any(),
                any(),
            )
        } returns objectMapper.readTree("{}")

        val result =
            fellesBehovLøserLøsninger.harSøkerenHattArbeidsforholdIEøs(
                beskrivendeId = "faktum.eos-arbeid-siste-36-mnd",
                ident = ident,
                søknadId = søknadId,
            )

        assertFalse { result }

        verify {
            opplysningRepository.hent(
                beskrivendeId = "faktum.eos-arbeid-siste-36-mnd",
                ident = ident,
                søknadId = søknadId,
            )
        }
        verify {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident = ident,
                søknadId = søknadId,
                seksjonId = "arbeidsforhold",
            )
        }
    }

    @Test
    fun `ønsker dagpenger fra dato i quiz opplysning`() {
        val forventetDato = LocalDate.now()
        val opplysning =
            QuizOpplysning(
                beskrivendeId = "faktum.dagpenger-soknadsdato",
                type = Dato,
                svar = forventetDato,
                ident = ident,
                søknadId = søknadId,
            )
        every { opplysningRepository.hent(any(), any(), any()) }.returns(opplysning)
        val result =
            fellesBehovLøserLøsninger.ønskerDagpengerFraDato(
                ident = ident,
                søknadId = søknadId,
                behov = "Søknadsdata",
            )

        assertTrue { result == forventetDato }
        verify {
            opplysningRepository.hent(
                beskrivendeId = "faktum.dagpenger-soknadsdato",
                ident = ident,
                søknadId = søknadId,
            )
        }

        verify(exactly = 0) {
            opplysningRepository.hent(
                beskrivendeId = "faktum.arbeidsforhold.gjenopptak.soknadsdato-gjenopptak",
                ident = ident,
                søknadId = søknadId,
            )
        }
    }

    @Test
    fun `Ønsker gjennopptaksdato for dagpenger fra dato i quiz opplysning`() {
        val forventetDato = LocalDate.now()
        val opplysning =
            QuizOpplysning(
                beskrivendeId = "faktum.arbeidsforhold.gjenopptak.soknadsdato-gjenopptak",
                type = Dato,
                svar = forventetDato,
                ident = ident,
                søknadId = søknadId,
            )
        every { opplysningRepository.hent(any(), any(), any()) }.returns(null)
        every {
            opplysningRepository.hent(
                "faktum.arbeidsforhold.gjenopptak.soknadsdato-gjenopptak",
                any(),
                any(),
            )
        }.returns(opplysning)
        val result =
            fellesBehovLøserLøsninger.ønskerDagpengerFraDato(
                ident = ident,
                søknadId = søknadId,
                behov = "Søknadsdata",
            )

        assertTrue { result == forventetDato }
        verify {
            opplysningRepository.hent(
                beskrivendeId = "faktum.arbeidsforhold.gjenopptak.soknadsdato-gjenopptak",
                ident = ident,
                søknadId = søknadId,
            )
        }

        verify {
            opplysningRepository.hent(
                beskrivendeId = "faktum.dagpenger-soknadsdato",
                ident = ident,
                søknadId = søknadId,
            )
        }

        verify(exactly = 0) {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident = ident,
                søknadId = søknadId,
                seksjonId = "din-situasjon",
            )
        }
    }

    @Test
    fun `ønsker dagpenger for dagpenger fra dato i orkestrator opplysning`() {
        var forventetDato = LocalDate.now()
        every { opplysningRepository.hent(any(), any(), any()) }.returns(null)
        every { seksjonRepository.hentSeksjonsvarEllerKastException(any(), any(), any()) }.returns(
            objectMapper.readTree(
                """
                {
                    "harDuMottattDagpengerFraNavILøpetAvDeSiste52Ukene": "nei",
                    "hvilkenDatoSøkerDuDagpengerFra": "$forventetDato"              
                }
                """.trimIndent(),
            ),
        )

        val resultat =
            fellesBehovLøserLøsninger.ønskerDagpengerFraDato(
                ident = ident,
                søknadId = søknadId,
                behov = "Søknadsdata",
            )
        assertTrue { resultat == forventetDato }
        verify {
            opplysningRepository.hent(
                beskrivendeId = "faktum.dagpenger-soknadsdato",
                ident = ident,
                søknadId = søknadId,
            )
        }

        verify {
            opplysningRepository.hent(
                beskrivendeId = "faktum.arbeidsforhold.gjenopptak.soknadsdato-gjenopptak",
                ident = ident,
                søknadId = søknadId,
            )
        }
        verify {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident = ident,
                søknadId = søknadId,
                seksjonId = "din-situasjon",
            )
        }
    }

    @Test
    fun `Ønsker gjennopptaksdato for dagpenger fra dato i orkestrator opplysning`() {
        val forventetDato = LocalDate.now()
        every { opplysningRepository.hent(any(), any(), any()) }.returns(null)
        every { seksjonRepository.hentSeksjonsvarEllerKastException(any(), any(), any()) }.returns(
            objectMapper.readTree(
                """
                {
                    "hvilkenDatoSøkerDuGjenopptakFra": "$forventetDato"
                }
                """.trimIndent(),
            ),
        )

        val resultat =
            fellesBehovLøserLøsninger.ønskerDagpengerFraDato(
                ident = ident,
                søknadId = søknadId,
                behov = "Søknadsdata",
            )

        assertTrue { resultat == forventetDato }
        verify {
            opplysningRepository.hent(
                beskrivendeId = "faktum.dagpenger-soknadsdato",
                ident = ident,
                søknadId = søknadId,
            )
        }

        verify {
            opplysningRepository.hent(
                beskrivendeId = "faktum.arbeidsforhold.gjenopptak.soknadsdato-gjenopptak",
                ident = ident,
                søknadId = søknadId,
            )
        }
        verify {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident = ident,
                søknadId = søknadId,
                seksjonId = "din-situasjon",
            )
        }
    }

    @Test
    fun `Dagpenger fra dato kaster exception når seksjonRepository kaster exception`() {
        every { opplysningRepository.hent(any(), any(), any()) }.returns(null)
        every { seksjonRepository.hentSeksjonsvarEllerKastException(any(), any(), any()) } throws
            IllegalStateException("Fant ingen seksjonsvar på din-situasjon for søknad $søknadId")

        shouldThrow<IllegalStateException> {
            fellesBehovLøserLøsninger.ønskerDagpengerFraDato(
                ident = ident,
                søknadId = søknadId,
                behov = "Søknadsdata",
            )
        }

        verify {
            opplysningRepository.hent(
                beskrivendeId = "faktum.dagpenger-soknadsdato",
                ident = ident,
                søknadId = søknadId,
            )
        }

        verify {
            opplysningRepository.hent(
                beskrivendeId = "faktum.arbeidsforhold.gjenopptak.soknadsdato-gjenopptak",
                ident = ident,
                søknadId = søknadId,
            )
        }
        verify {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident = ident,
                søknadId = søknadId,
                seksjonId = "din-situasjon",
            )
        }
    }

    @Test
    fun `Dagpenger fra dato kaster exception når verdien ikke finnes`() {
        every { opplysningRepository.hent(any(), any(), any()) }.returns(null)
        every { seksjonRepository.hentSeksjonsvarEllerKastException(any(), any(), any()) } returns objectMapper.readTree("")

        shouldThrow<IllegalStateException> {
            fellesBehovLøserLøsninger.ønskerDagpengerFraDato(
                ident = ident,
                søknadId = søknadId,
                behov = "Søknadsdata",
            )
        }

        verify {
            opplysningRepository.hent(
                beskrivendeId = "faktum.dagpenger-soknadsdato",
                ident = ident,
                søknadId = søknadId,
            )
        }

        verify {
            opplysningRepository.hent(
                beskrivendeId = "faktum.arbeidsforhold.gjenopptak.soknadsdato-gjenopptak",
                ident = ident,
                søknadId = søknadId,
            )
        }
        verify {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident = ident,
                søknadId = søknadId,
                seksjonId = "din-situasjon",
            )
        }
    }

    @Test
    fun `Om søkeren har avtjent verneplikt fra quiz`() {
        val cases = listOf(Pair(true, true), Pair(false, false))
        cases.forEach { (avtjentVerneplikt, forventedSvar) ->

            val opplysning =
                QuizOpplysning(
                    beskrivendeId = "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd",
                    type = Boolsk,
                    svar = avtjentVerneplikt,
                    ident = ident,
                    søknadId = søknadId,
                )
            every {
                opplysningRepository.hent(
                    any(),
                    any(),
                    any(),
                )
            }.returns(opplysning)

            val result =
                fellesBehovLøserLøsninger.harSøkerenAvtjentVerneplikt(
                    behov = "Verneplikt",
                    beskrivendeId = "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd",
                    ident = ident,
                    søknadId = søknadId,
                )

            assertTrue { result == forventedSvar }

            verify {
                opplysningRepository.hent(
                    beskrivendeId = "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd",
                    ident = ident,
                    søknadId = søknadId,
                )
            }
        }
    }

    @Test
    fun `Om søkeren har avtjent verneplikt fra orkestrator`() {
        val cases = listOf(Pair(true, true), Pair(false, false))
        cases.forEach { (avtjentVerneplikt, forventedSvar) ->
            val vernepliktSvar = if (avtjentVerneplikt) "ja" else "nei"
            every { opplysningRepository.hent(any(), any(), any()) }.returns(null)

            every {
                seksjonRepository.hentSeksjonsvarEllerKastException(
                    any(),
                    any(),
                    any(),
                )
            } returns
                objectMapper.readTree(
                    """
                    {
                        "avtjentVerneplikt": "$vernepliktSvar"
                    }
                    """.trimIndent(),
                )

            val result =
                fellesBehovLøserLøsninger.harSøkerenAvtjentVerneplikt(
                    behov = "Verneplikt",
                    beskrivendeId = "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd",
                    ident = ident,
                    søknadId = søknadId,
                )

            assertTrue { result == forventedSvar }

            verify {
                opplysningRepository.hent(
                    beskrivendeId = "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd",
                    ident = ident,
                    søknadId = søknadId,
                )
            }

            verify {
                seksjonRepository.hentSeksjonsvarEllerKastException(
                    ident = ident,
                    søknadId = søknadId,
                    seksjonId = "verneplikt",
                )
            }
        }
    }

    @Test
    fun `Om søkeren har avtjent verneplikt når harSøkerenAvtjentVerneplikt kaster exception`() {
        every { opplysningRepository.hent(any(), any(), any()) }.returns(null)
        every { seksjonRepository.hentSeksjonsvarEllerKastException(any(), any(), any()) } throws
            IllegalStateException("Fant ingen seksjonsvar på verneplikt for søknad $søknadId")

        shouldThrow<IllegalStateException> {
            fellesBehovLøserLøsninger.harSøkerenAvtjentVerneplikt(
                behov = "Verneplikt",
                beskrivendeId = "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd",
                ident = ident,
                søknadId = søknadId,
            )
        }

        verify {
            opplysningRepository.hent(
                beskrivendeId = "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd",
                ident = ident,
                søknadId = søknadId,
            )
        }

        verify {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident = ident,
                søknadId = søknadId,
                seksjonId = "verneplikt",
            )
        }
    }

    @Test
    fun `Om søkeren har avtjent verneplikt kaster exception når verdien ikke finnes`() {
        every { opplysningRepository.hent(any(), any(), any()) }.returns(null)
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                any(),
                any(),
                any(),
            )
        } returns objectMapper.readTree("")

        shouldThrow<IllegalStateException> {
            fellesBehovLøserLøsninger.harSøkerenAvtjentVerneplikt(
                behov = "Verneplikt",
                beskrivendeId = "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd",
                ident = ident,
                søknadId = søknadId,
            )
        }

        verify {
            opplysningRepository.hent(
                beskrivendeId = "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd",
                ident = ident,
                søknadId = søknadId,
            )
        }

        verify {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident = ident,
                søknadId = søknadId,
                seksjonId = "verneplikt",
            )
        }
    }
}
