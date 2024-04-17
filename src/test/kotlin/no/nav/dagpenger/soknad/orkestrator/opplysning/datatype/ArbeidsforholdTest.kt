package no.nav.dagpenger.soknad.orkestrator.opplysning.datatype

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Sluttårsak
import kotlin.test.Test

class ArbeidsforholdTest {
    @Test
    fun `Setter riktig sluttårsak ved mapping til opplysning`() {
        Arbeidsforhold.finnSluttårsak(arbeidsforholdPermittertFraFiskeri) shouldBe Sluttårsak.PERMITTERT_FISKEFOREDLING
        Arbeidsforhold.finnSluttårsak(arbeidsforholdPermittert) shouldBe Sluttårsak.PERMITTERT
        Arbeidsforhold.finnSluttårsak(arbeidsforholdAvskjediget) shouldBe Sluttårsak.AVSKJEDIGET
        Arbeidsforhold.finnSluttårsak(arbeidsforholdKontraktUtgått) shouldBe Sluttårsak.KONTRAKT_UTGAATT
        Arbeidsforhold.finnSluttårsak(arbeidsforholdRedusertArbeidstid) shouldBe Sluttårsak.REDUSERT_ARBEIDSTID
        Arbeidsforhold.finnSluttårsak(arbeidsforholdSagtOppAvArbeidsgiver) shouldBe Sluttårsak.SAGT_OPP_AV_ARBEIDSGIVER
        Arbeidsforhold.finnSluttårsak(arbeidsforholdSagtOppSelv) shouldBe Sluttårsak.SAGT_OPP_SELV
        Arbeidsforhold.finnSluttårsak(arbeidsforholdIkkeEndret) shouldBe Sluttårsak.IKKE_ENDRET
    }
}

//language=JSON
val arbeidsforholdPermittertFraFiskeri =
    ObjectMapper().readTree(
        """
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
            "svar": "faktum.arbeidsforhold.endret.svar.permittert",
            "type": "envalg",
            "beskrivendeId": "faktum.arbeidsforhold.endret"
          },
          {
            "svar": true,
            "type": "boolean",
            "beskrivendeId": "faktum.arbeidsforhold.permittertert-fra-fiskeri-naering"
          }
        ]
        """.trimIndent(),
    )

//language=JSON
val arbeidsforholdPermittert =
    ObjectMapper().readTree(
        """
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
            "svar": "faktum.arbeidsforhold.endret.svar.permittert",
            "type": "envalg",
            "beskrivendeId": "faktum.arbeidsforhold.endret"
          },
          {
            "svar": false,
            "type": "boolean",
            "beskrivendeId": "faktum.arbeidsforhold.permittertert-fra-fiskeri-naering"
          }
        ]
        """.trimIndent(),
    )

//language=JSON
val arbeidsforholdAvskjediget =
    ObjectMapper().readTree(
        """
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
            "svar": "faktum.arbeidsforhold.endret.svar.avskjediget",
            "type": "envalg",
            "beskrivendeId": "faktum.arbeidsforhold.endret"
          }
        ]
        """.trimIndent(),
    )

//language=JSON
val arbeidsforholdKontraktUtgått =
    ObjectMapper().readTree(
        """
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
            "svar": "faktum.arbeidsforhold.endret.svar.kontrakt-utgaatt",
            "type": "envalg",
            "beskrivendeId": "faktum.arbeidsforhold.endret"
          }
        ]
        """.trimIndent(),
    )

//language=JSON
val arbeidsforholdRedusertArbeidstid =
    ObjectMapper().readTree(
        """
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
            "svar": "faktum.arbeidsforhold.endret.svar.redusert-arbeidstid",
            "type": "envalg",
            "beskrivendeId": "faktum.arbeidsforhold.endret"
          }
        ]
        """.trimIndent(),
    )

//language=JSON
val arbeidsforholdSagtOppAvArbeidsgiver =
    ObjectMapper().readTree(
        """
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
            "svar": false,
            "type": "boolean",
            "beskrivendeId": "faktum.arbeidsforhold.permittertert-fra-fiskeri-naering"
          }
        ]
        """.trimIndent(),
    )

//language=JSON
val arbeidsforholdSagtOppSelv =
    ObjectMapper().readTree(
        """
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
            "svar": "faktum.arbeidsforhold.endret.svar.sagt-opp-selv",
            "type": "envalg",
            "beskrivendeId": "faktum.arbeidsforhold.endret"
          },
          {
            "svar": false,
            "type": "boolean",
            "beskrivendeId": "faktum.arbeidsforhold.permittertert-fra-fiskeri-naering"
          }
        ]
        """.trimIndent(),
    )

//language=JSON
val arbeidsforholdIkkeEndret =
    ObjectMapper().readTree(
        """
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
            "svar": "faktum.arbeidsforhold.endret.svar.ikke-endret",
            "type": "envalg",
            "beskrivendeId": "faktum.arbeidsforhold.endret"
          },
          {
            "svar": false,
            "type": "boolean",
            "beskrivendeId": "faktum.arbeidsforhold.permittertert-fra-fiskeri-naering"
          }
        ]
        """.trimIndent(),
    )
