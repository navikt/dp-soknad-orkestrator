package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.api.models.OppdatertBarnRequestDTO
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.beskrivendeIdEgneBarn
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.beskrivendeIdPdlBarn
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Barn
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class OpplysningServiceTest {
    private val opplysningRepository = mockk<QuizOpplysningRepository>()
    private val opplysningService = OpplysningService(opplysningRepository)

    @Test
    fun `hentBarn returnerer en liste med register og egne barn som BarnResponseDTO`() {
        val søknadId = UUID.randomUUID()
        val registerBarn =
            BarnSvar(
                barnSvarId = UUID.randomUUID(),
                fornavnOgMellomnavn = "Kari Register",
                etternavn = "Nordamm",
                fødselsdato = LocalDate.of(2020, 1, 1),
                statsborgerskap = "NOR",
                forsørgerBarnet = false,
                fraRegister = true,
                kvalifisererTilBarnetillegg = false,
            )

        val egetBarn =
            BarnSvar(
                barnSvarId = UUID.randomUUID(),
                fornavnOgMellomnavn = "Kari Eget",
                etternavn = "Nordmann",
                fødselsdato = LocalDate.of(2020, 1, 1),
                statsborgerskap = "NOR",
                forsørgerBarnet = false,
                fraRegister = false,
                kvalifisererTilBarnetillegg = false,
            )
        every { opplysningRepository.hent(beskrivendeIdPdlBarn, søknadId) } returns
            QuizOpplysning(
                beskrivendeId = beskrivendeIdPdlBarn,
                type = Barn,
                svar = listOf(registerBarn),
                ident = "12345678910",
                søknadId = søknadId,
            )

        every { opplysningRepository.hent(beskrivendeIdEgneBarn, søknadId) } returns
            QuizOpplysning(
                beskrivendeId = beskrivendeIdPdlBarn,
                type = Barn,
                svar = listOf(egetBarn),
                ident = "12345678910",
                søknadId = søknadId,
            )

        val hentedeBarn = opplysningService.hentBarn(søknadId)

        hentedeBarn.size shouldBe 2
        hentedeBarn.first().fornavnOgMellomnavn shouldBe "Kari Register"
        hentedeBarn.last().fornavnOgMellomnavn shouldBe "Kari Eget"
    }

    @Test
    fun `hentBarn returnerer en tom liste hvis det ikke finnes noen barn for gitt søknadId`() {
        val søknadId = UUID.randomUUID()
        every { opplysningRepository.hent(any(), søknadId) } returns null

        val hentedeBarn = opplysningService.hentBarn(søknadId)

        hentedeBarn shouldBe emptyList()
    }

    @Test
    fun `erEndret returnerer true dersom dersom det er gjort endringer`() {
        val søknadId = UUID.randomUUID()
        val barnId = UUID.randomUUID()

        val opprinneligBarn =
            BarnSvar(
                barnSvarId = barnId,
                fornavnOgMellomnavn = "Kari",
                etternavn = "Nordmann",
                fødselsdato = LocalDate.of(2020, 1, 1),
                statsborgerskap = "NOR",
                forsørgerBarnet = false,
                fraRegister = false,
                kvalifisererTilBarnetillegg = false,
            )
        val oppdatertBarn =
            OppdatertBarnRequestDTO(
                barnId = barnId,
                fornavnOgMellomnavn = "Kari",
                etternavn = "Nordmann",
                fødselsdato = LocalDate.of(2020, 1, 1),
                oppholdssted = "NOR",
                forsørgerBarnet = true,
                kvalifisererTilBarnetillegg = true,
                barnetilleggFom = LocalDate.of(2020, 1, 1),
                barnetilleggTom = LocalDate.of(2038, 1, 1),
                begrunnelse = "Begrunnelse",
            )

        every { opplysningRepository.hent(beskrivendeIdPdlBarn, søknadId) } returns
            QuizOpplysning(
                beskrivendeId = beskrivendeIdPdlBarn,
                type = Barn,
                svar = listOf(opprinneligBarn),
                ident = "12345678910",
                søknadId = søknadId,
            )

        every { opplysningRepository.hent(beskrivendeIdEgneBarn, søknadId) } returns null

        opplysningService.erEndret(oppdatertBarn, søknadId) shouldBe true
    }

    @Test
    fun `erEndret returnerer false dersom dersom det ikke er gjort andre endringer enn begrunnelse`() {
        val søknadId = UUID.randomUUID()
        val barnId = UUID.randomUUID()

        val opprinneligBarn =
            BarnSvar(
                barnSvarId = barnId,
                fornavnOgMellomnavn = "Kari",
                etternavn = "Nordmann",
                fødselsdato = LocalDate.of(2020, 1, 1),
                statsborgerskap = "NOR",
                forsørgerBarnet = false,
                fraRegister = false,
                kvalifisererTilBarnetillegg = false,
            )
        val oppdatertBarn =
            OppdatertBarnRequestDTO(
                barnId = barnId,
                fornavnOgMellomnavn = "Kari",
                etternavn = "Nordmann",
                fødselsdato = LocalDate.of(2020, 1, 1),
                oppholdssted = "NOR",
                forsørgerBarnet = false,
                kvalifisererTilBarnetillegg = false,
                begrunnelse = "Begrunnelse",
            )

        every { opplysningRepository.hent(beskrivendeIdPdlBarn, søknadId) } returns
            QuizOpplysning(
                beskrivendeId = beskrivendeIdPdlBarn,
                type = Barn,
                svar = listOf(opprinneligBarn),
                ident = "12345678910",
                søknadId = søknadId,
            )

        every { opplysningRepository.hent(beskrivendeIdEgneBarn, søknadId) } returns null

        opplysningService.erEndret(oppdatertBarn, søknadId) shouldBe false
    }

    @Test
    fun `oppdaterBarn oppdaterer barn`() {
        val søknadId = UUID.randomUUID()
        val barnId = UUID.randomUUID()
        val opprinneligBarnSvar =
            BarnSvar(
                barnSvarId = barnId,
                fornavnOgMellomnavn = "Opprinnelig Navn",
                etternavn = "Opprinnelig Etternavn",
                fødselsdato = LocalDate.of(2010, 1, 1),
                statsborgerskap = "NOR",
                forsørgerBarnet = false,
                fraRegister = true,
                kvalifisererTilBarnetillegg = false,
                barnetilleggFom = null,
                barnetilleggTom = null,
                begrunnelse = null,
                endretAv = null,
            )
        val oppdatertBarnRequest =
            OppdatertBarnRequestDTO(
                barnId = barnId,
                fornavnOgMellomnavn = "Oppdatert Navn",
                etternavn = "Oppdatert Etternavn",
                fødselsdato = LocalDate.of(2010, 1, 1),
                oppholdssted = "NOR",
                forsørgerBarnet = true,
                kvalifisererTilBarnetillegg = true,
                barnetilleggFom = LocalDate.of(2020, 1, 1),
                barnetilleggTom = LocalDate.of(2038, 1, 1),
                begrunnelse = "Begrunnelse",
            )
        val opprinneligOpplysning =
            QuizOpplysning(
                beskrivendeId = beskrivendeIdPdlBarn,
                type = Barn,
                svar = listOf(opprinneligBarnSvar),
                ident = "12345678910",
                søknadId = søknadId,
            )

        every { opplysningRepository.hentAlle(søknadId) } returns listOf(opprinneligOpplysning)
        every { opplysningRepository.oppdaterBarn(søknadId, any()) } returns Unit

        opplysningService.oppdaterBarn(oppdatertBarnRequest, søknadId, "saksbehandlerId")

        verify {
            opplysningRepository.oppdaterBarn(
                søknadId,
                match {
                    it.barnSvarId == barnId &&
                        it.fornavnOgMellomnavn == "Oppdatert Navn" &&
                        it.etternavn == "Oppdatert Etternavn" &&
                        it.fødselsdato == LocalDate.of(2010, 1, 1) &&
                        it.statsborgerskap == "NOR" &&
                        it.forsørgerBarnet == true &&
                        it.kvalifisererTilBarnetillegg == true &&
                        it.barnetilleggFom == LocalDate.of(2020, 1, 1) &&
                        it.barnetilleggTom == LocalDate.of(2038, 1, 1) &&
                        it.begrunnelse == "Begrunnelse" &&
                        it.endretAv == "saksbehandlerId"
                },
            )
        }
    }
}
