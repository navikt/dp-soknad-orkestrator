package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Permittert
import no.nav.dagpenger.soknad.orkestrator.behov.permittering.PermittertVurdering
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.Test

class PermittertBehovløserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val testRapid = TestRapid()
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    val permittering = PermittertVurdering(opplysningRepository, seksjonRepository)
    val behovløser = PermittertBehovløser(testRapid, opplysningRepository, søknadRepository, seksjonRepository)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov Permittert med verdi og gjelderFra`() {
        opplysningRepository.lagre(opplysning())

        // Må også lagre søknadstidspunkt fordi det er denne som brukes for å sette gjelderFra i første omgang
        val søknadstidspunkt = ZonedDateTime.now()
        val søknadstidpsunktOpplysning =
            QuizOpplysning(
                beskrivendeId = "søknadstidspunkt",
                type = Tekst,
                svar = søknadstidspunkt.toString(),
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(søknadstidpsunktOpplysning)
        behovløser.løs(lagBehovmelding(ident, søknadId, Permittert))

        testRapid.inspektør.message(0)["@løsning"]["Permittert"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe true
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Behovløser publiserer løsning på behov Permittert med verdi og gjelderFra fra seksjonsdata`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                any(),
                any(),
                any(),
            )
        } returns
            """
            {
                "seksjonId": "arbeidsforhold",
                "seksjonsvar": {
                "registrerteArbeidsforhold": [
                    {
                        "hvordanHarDetteArbeidsforholdetEndretSeg": "jegErPermittert"
                    }
                ]
                },
                "versjon": 1
            }
            """.trimIndent()

        // Må også lagre søknadstidspunkt fordi det er denne som brukes for å sette gjelderFra i første omgang
        val søknadstidspunkt = ZonedDateTime.now()
        every {
            søknadRepository.hent(any())
        } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )
        behovløser.løs(lagBehovmelding(ident, søknadId, Permittert))

        verify { seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "arbeidsforhold") }
        verify { søknadRepository.hent(søknadId) }
        testRapid.inspektør.message(0)["@løsning"]["Permittert"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe true
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `Behovløser publiserer løsning på behov Permittert med verdi og gjelderFra fra seksjonsdata hvor sluttårsak var annet en permittert`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                any(),
                any(),
                any(),
            )
        } returns
            """
            {
                "seksjonId": "arbeidsforhold",
                "seksjonsvar": {
                "registrerteArbeidsforhold": [
                    {
                        "hvordanHarDetteArbeidsforholdetEndretSeg": "arbeidsgiverErKonkurs"
                    }
                ]
                },
                "versjon": 1
            }
            """.trimIndent()

        // Må også lagre søknadstidspunkt fordi det er denne som brukes for å sette gjelderFra i første omgang
        val søknadstidspunkt = ZonedDateTime.now()
        every {
            søknadRepository.hent(any())
        } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )
        behovløser.løs(lagBehovmelding(ident, søknadId, Permittert))

        verify { seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "arbeidsforhold") }
        verify { søknadRepository.hent(søknadId) }
        testRapid.inspektør.message(0)["@løsning"]["Permittert"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe false
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Behovløser publiserer løsning på behov Permittert med verdi og gjelderFra fra seksjonsdata uten arbeidsforhold gir false`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                any(),
                any(),
                any(),
            )
        } returns
            """
            {
                "seksjonId": "arbeidsforhold",
                "seksjonsvar": {
                "registrerteArbeidsforhold": [
                    {
                        "DetteErIkkeEkteNøkkel": "arbeidsgiverErKonkurs"
                    }
                ]
                },
                "versjon": 1
            }
            """.trimIndent()

        // Må også lagre søknadstidspunkt fordi det er denne som brukes for å sette gjelderFra i første omgang
        val søknadstidspunkt = ZonedDateTime.now()
        every {
            søknadRepository.hent(any())
        } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )
        behovløser.løs(lagBehovmelding(ident, søknadId, Permittert))

        verify { seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "arbeidsforhold") }
        verify { søknadRepository.hent(søknadId) }
        testRapid.inspektør.message(0)["@løsning"]["Permittert"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe false
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `Behovløser publiserer løsning på behov Permittert med verdi og gjelderFra med arbeidsforhold uten hvordanHarDetteArbeidsforholdetEndretSeg`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                any(),
                any(),
                any(),
            )
        } returns
            """
            {
                "seksjonId": "arbeidsforhold",
                "seksjonsvar": {
                "registrerteArbeidsforhold": []
                },
                "versjon": 1
            }
            """.trimIndent()

        // Må også lagre søknadstidspunkt fordi det er denne som brukes for å sette gjelderFra i første omgang
        val søknadstidspunkt = ZonedDateTime.now()
        every {
            søknadRepository.hent(any())
        } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )
        behovløser.løs(lagBehovmelding(ident, søknadId, Permittert))

        verify { seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "arbeidsforhold") }
        verify { søknadRepository.hent(søknadId) }
        testRapid.inspektør.message(0)["@løsning"]["Permittert"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe false
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Behovløser setter løsning til true når minst ett arbeidsforhold har sluttårsak PERMITTERT`() {
        val svarMedEttPermittertArbeidsforhold =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold1",
                    land = "NOR",
                    sluttårsak = Sluttårsak.PERMITTERT,
                ),
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold2",
                    land = "NOR",
                    sluttårsak = Sluttårsak.KONTRAKT_UTGAATT,
                ),
            )

        opplysningRepository.lagre(opplysning(svar = svarMedEttPermittertArbeidsforhold))
        permittering.rettTilDagpenger(ident, søknadId) shouldBe true
    }

    @Test
    fun `Behovløser setter løsning til false når ingen arbeidsforhold har sluttårsak PERMITTERT`() {
        val svarUtenPermittertArbeidsforhold =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold",
                    land = "NOR",
                    sluttårsak = Sluttårsak.ARBEIDSGIVER_KONKURS,
                ),
            )

        opplysningRepository.lagre(opplysning(svar = svarUtenPermittertArbeidsforhold))
        permittering.rettTilDagpenger(ident, søknadId) shouldBe false
    }

    @Test
    fun `Behovløser setter løsning til false når det ikke er noen opplysning om arbeidsforhold`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                any(),
                any(),
                any(),
            )
        } throws IllegalStateException("Fant ingen seksjonsvar på arbeidsforhold for søknad=$søknadId")
        permittering.rettTilDagpenger(ident, søknadId) shouldBe false
    }

    @Test
    fun `quiz med vanlig permittering og fiskepermittering gir false`() {
        opplysningRepository.lagre(
            opplysning(listOf(arbeidsforhold(Sluttårsak.PERMITTERT), arbeidsforhold(Sluttårsak.PERMITTERT_FISKEFOREDLING))),
        )

        val resultat = permittering.rettTilDagpenger(ident, søknadId)

        resultat shouldBe false
    }

    @Test
    fun `quiz med bare fiskepermittering gir false`() {
        opplysningRepository.lagre(opplysning(listOf(arbeidsforhold(Sluttårsak.PERMITTERT_FISKEFOREDLING))))

        val resultat = permittering.rettTilDagpenger(ident, søknadId)

        resultat shouldBe false
    }

    @Test
    fun `seksjon med vanlig permittering og fiskepermittering gir false`() {
        mockSeksjonssvar(listOf(seksjonsarbeidsforhold(fisk = "nei"), seksjonsarbeidsforhold(fisk = "ja")))

        val resultat = permittering.rettTilDagpenger(ident, søknadId)

        resultat shouldBe false
    }

    @Test
    fun `seksjon med bare fiskepermittering gir false`() {
        mockSeksjonssvar(listOf(seksjonsarbeidsforhold(fisk = "ja")))

        val resultat = permittering.rettTilDagpenger(ident, søknadId)

        resultat shouldBe false
    }

    @Test
    fun `seksjon med vanlig permittering og fiskefelt nei gir true`() {
        mockSeksjonssvar(listOf(seksjonsarbeidsforhold(fisk = "nei")))

        val resultat = permittering.rettTilDagpenger(ident, søknadId)

        resultat shouldBe true
    }

    @Test
    fun `skal ignorere fiskesvar på arbeidsforhold som ikke er permittert`() {
        mockSeksjonssvar(
            listOf(
                seksjonsarbeidsforhold(endring = "arbeidsgiverErKonkurs", fisk = "ja"),
                seksjonsarbeidsforhold(),
            ),
        )

        val resultat = permittering.rettTilDagpenger(ident, søknadId)

        resultat shouldBe true
    }

    @Test
    fun `quiz har prioritet over seksjonsdata også med tom quizliste`() {
        opplysningRepository.lagre(opplysning(emptyList()))
        mockSeksjonssvar(listOf(seksjonsarbeidsforhold(fisk = "nei")))

        val resultat = permittering.rettTilDagpenger(ident, søknadId)

        resultat shouldBe false
        verify(exactly = 0) { seksjonRepository.hentSeksjonsvarEllerKastException(any(), any(), any()) }
    }

    private fun arbeidsforhold(sluttårsak: Sluttårsak) = ArbeidsforholdSvar(navn = "arbeidsforhold", land = "NOR", sluttårsak = sluttårsak)

    private fun seksjonsarbeidsforhold(
        endring: String = "jegErPermittert",
        fisk: String? = "nei",
    ) = mapOf(
        "hvordanHarDetteArbeidsforholdetEndretSeg" to endring,
        "permittertErDuPermittertFraFiskeforedlingsEllerFiskeoljeindustrien" to fisk,
    )

    private fun mockSeksjonssvar(arbeidsforhold: List<Map<String, String?>>) {
        every { seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "arbeidsforhold") } returns
            objectMapper.writeValueAsString(
                mapOf(
                    "seksjonId" to "arbeidsforhold",
                    "seksjonsvar" to mapOf("registrerteArbeidsforhold" to arbeidsforhold),
                    "versjon" to 1,
                ),
            )
    }

    private fun opplysning(
        svar: List<ArbeidsforholdSvar> =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold",
                    land = "NOR",
                    sluttårsak = Sluttårsak.PERMITTERT,
                ),
            ),
    ): QuizOpplysning<List<ArbeidsforholdSvar>> {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Arbeidsforhold,
                svar = svar,
                ident = ident,
                søknadId = søknadId,
            )
        return opplysning
    }
}
