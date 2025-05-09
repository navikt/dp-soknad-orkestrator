package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.PermittertGrensearbeiderBehovløser.Companion.reistITaktMedRotasjonBeskrivendeId
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.PermittertGrensearbeiderBehovløser.Companion.reistTilbakeEnGangEllerMerBeskrivendeId
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Boolsk
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.ZonedDateTime
import java.util.UUID

class PermittertGrensearbeiderBehovløserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val testRapid = TestRapid()
    val behovløser = PermittertGrensearbeiderBehovløser(testRapid, opplysningRepository)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @ParameterizedTest
    @MethodSource("testtilfeller")
    fun `Behovløser publiserer løsning på behov PermittertGrensearbeider med verdi og gjelderFra`(
        testData: Triple<Boolean?, Boolean?, Boolean>,
    ) {
        val forventetSvar = testData.third

        testData.first?.let {
            val opplysning = generateReistTilbakeEnGangIUkenOpplysning(it)
            opplysningRepository.lagre(opplysning)
        }
        testData.second?.let {
            val opplysning = generateReistITaktMedRotasjonOpplysning(it)
            opplysningRepository.lagre(opplysning)
        }

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

        behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.PermittertGrensearbeider))

        testRapid.inspektør.message(0)["@løsning"]["PermittertGrensearbeider"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe forventetSvar
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    fun generateReistTilbakeEnGangIUkenOpplysning(svar: Boolean): QuizOpplysning<*> =
        generateOpplysning(
            beskrivendeId = reistTilbakeEnGangEllerMerBeskrivendeId,
            svar = svar,
        )

    fun generateReistITaktMedRotasjonOpplysning(svar: Boolean): QuizOpplysning<*> =
        generateOpplysning(
            beskrivendeId = reistITaktMedRotasjonBeskrivendeId,
            svar = svar,
        )

    fun generateOpplysning(
        beskrivendeId: String,
        svar: Boolean,
    ): QuizOpplysning<*> =
        QuizOpplysning(
            beskrivendeId = beskrivendeId,
            type = Boolsk,
            svar = svar,
            ident = ident,
            søknadId = søknadId,
        )

    companion object {
        @JvmStatic
        fun testtilfeller() =
            listOf(
                Triple(true, true, true),
                Triple(true, false, true),
                Triple(false, true, true),
                Triple(false, false, false),
                Triple(true, null, true),
                Triple(null, null, false),
            )
    }
}
