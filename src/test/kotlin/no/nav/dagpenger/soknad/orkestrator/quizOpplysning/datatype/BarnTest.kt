package no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatype

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Barn.barnetilleggperiode
import java.time.LocalDate
import kotlin.test.Test

class BarnTest {
    @Test
    fun `Vi setter riktig periode for barnetillegg`() {
        val fødselsdato = LocalDate.of(2000, 1, 1)
        val forventetBarnetillegTom = LocalDate.of(2018, 1, 1)

        val barnetilleggperiode = barnetilleggperiode(fødselsdato)

        barnetilleggperiode.first shouldBe fødselsdato
        barnetilleggperiode.second shouldBe forventetBarnetillegTom
    }
}
