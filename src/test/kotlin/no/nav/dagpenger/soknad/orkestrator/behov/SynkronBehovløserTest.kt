package no.nav.dagpenger.soknad.orkestrator.behov

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import org.junit.jupiter.api.Test
import java.util.UUID

class SynkronBehovløserTest {
    private val opplysningRepository = InMemoryQuizOpplysningRepository()
    private val ident = "12345678910"
    private val søknadId = UUID.randomUUID()
    private val søknadRepository =
        mockk<SøknadRepository>().also {
            every { it.hent(any()) } returns null
            every { it.hent(søknadId) } returns Søknad(søknadId = søknadId, ident = ident)
        }
    private val synkronBehovløser =
        SynkronBehovløser(opplysningRepository, mockk<SeksjonRepository>(relaxed = true), søknadRepository)

    @Test
    fun `Permittert er true for vanlig permittering`() {
        lagreArbeidsforhold(Sluttårsak.PERMITTERT)

        synkronBehovløser.løs(Behov.Permittert, ident, søknadId) shouldBe true
        synkronBehovløser.løs(Behov.PermittertFiskeforedling, ident, søknadId) shouldBe false
    }

    @Test
    fun `PermittertFiskeforedling er true for fiskepermittering`() {
        lagreArbeidsforhold(Sluttårsak.PERMITTERT_FISKEFOREDLING)

        synkronBehovløser.løs(Behov.PermittertFiskeforedling, ident, søknadId) shouldBe true
        synkronBehovløser.løs(Behov.Permittert, ident, søknadId) shouldBe false
    }

    @Test
    fun `søknad som ikke finnes kaster SøknadFinnesIkke`() {
        shouldThrow<SøknadFinnesIkke> { synkronBehovløser.løs(Behov.Permittert, ident, UUID.randomUUID()) }
    }

    @Test
    fun `søknad som tilhører en annen ident kaster SøknadFinnesIkke`() {
        shouldThrow<SøknadFinnesIkke> { synkronBehovløser.løs(Behov.Permittert, "10987654321", søknadId) }
    }

    @Test
    fun `behov som ikke støttes kaster IkkeStøttetBehov`() {
        shouldThrow<IkkeStøttetBehov> { synkronBehovløser.løs(Behov.Ordinær, ident, søknadId) }
    }

    private fun lagreArbeidsforhold(sluttårsak: Sluttårsak) =
        opplysningRepository.lagre(
            QuizOpplysning(
                beskrivendeId = "faktum.arbeidsforhold",
                type = Arbeidsforhold,
                svar = listOf(ArbeidsforholdSvar(navn = "arbeidsforhold", land = "NOR", sluttårsak = sluttårsak)),
                ident = ident,
                søknadId = søknadId,
            ),
        )
}
