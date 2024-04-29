package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Ordinær
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryOpplysningRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import java.util.UUID
import kotlin.test.Ignore
import kotlin.test.Test

@Suppress("ktlint:standard:max-line-length")
class OrdinærBehovløserTest {
    val opplysningRepository = InMemoryOpplysningRepository()
    val testRapid = TestRapid()
    val behovløser = OrdinærBehovløser(testRapid, opplysningRepository)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    @Ignore
    fun `Behovløser publiserer løsning på behov Ordinær`() {
        opplysningRepository.lagre(opplysning())
        behovløser.løs(lagBehovmelding(ident, søknadId, Ordinær))

        testRapid.inspektør.message(0)["@løsning"]["Ordinær"]["verdi"].asBoolean() shouldBe true
    }

    @Test
    fun `Behovløser setter løsning til true når ingen arbeidsforhold har en ikke-ordinær sluttårsak PERMITTERT, PERMITTERT_FISKEFOREDLING eller ARBEIDSGIVER_KONKURS`() {
        val svarMedOrdinærRettighetstype =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold1",
                    land = "NOR",
                    sluttårsak = Sluttårsak.KONTRAKT_UTGAATT,
                ),
            )

        opplysningRepository.lagre(opplysning(svar = svarMedOrdinærRettighetstype))
        behovløser.rettTilOrdinæreDagpenger(ident, søknadId) shouldBe true
    }

    @Test
    fun `Behovløser setter løsning til false når minst ett arbeidsforhold har en ikke-ordinær sluttårsak PERMITTERT, PERMITTERT_FISKEFOREDLING eller ARBEIDSGIVER_KONKURS`() {
        val svarMedIkkeOrdinærRettighetstype =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold",
                    land = "NOR",
                    sluttårsak = Sluttårsak.KONTRAKT_UTGAATT,
                ),
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold",
                    land = "NOR",
                    sluttårsak = Sluttårsak.PERMITTERT,
                ),
            )

        opplysningRepository.lagre(opplysning(svar = svarMedIkkeOrdinærRettighetstype))
        behovløser.rettTilOrdinæreDagpenger(ident, søknadId) shouldBe false
    }

    @Test
    fun `Behovløser setter løsning til false når det ikke er noen opplysning om arbeidsforhold`() {
        behovløser.rettTilOrdinæreDagpenger(ident, søknadId) shouldBe false
    }

    private fun opplysning(
        svar: List<ArbeidsforholdSvar> =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold",
                    land = "NOR",
                    sluttårsak = Sluttårsak.KONTRAKT_UTGAATT,
                ),
            ),
    ): Opplysning<List<ArbeidsforholdSvar>> {
        val opplysning =
            Opplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Arbeidsforhold,
                svar = svar,
                ident = ident,
                søknadId = søknadId,
            )
        return opplysning
    }
}
