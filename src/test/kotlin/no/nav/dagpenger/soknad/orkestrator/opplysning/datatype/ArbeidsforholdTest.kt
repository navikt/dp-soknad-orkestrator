package no.nav.dagpenger.soknad.orkestrator.opplysning.datatype

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Sluttårsak
import kotlin.test.Test

class ArbeidsforholdTest {
    @Test
    fun `Setter riktig sluttårsak ved mapping til opplysning`() {
        Arbeidsforhold.finnSluttårsak(permittertFraFiskeri) shouldBe Sluttårsak.PERMITTERT_FISKEFOREDLING
        Arbeidsforhold.finnSluttårsak(permittert) shouldBe Sluttårsak.PERMITTERT
        Arbeidsforhold.finnSluttårsak(avskjediget) shouldBe Sluttårsak.AVSKJEDIGET
        Arbeidsforhold.finnSluttårsak(kontraktUtgått) shouldBe Sluttårsak.KONTRAKT_UTGAATT
        Arbeidsforhold.finnSluttårsak(redusertArbeidstid) shouldBe Sluttårsak.REDUSERT_ARBEIDSTID
        Arbeidsforhold.finnSluttårsak(sagtOppAvArbeidsgiver) shouldBe Sluttårsak.SAGT_OPP_AV_ARBEIDSGIVER
        Arbeidsforhold.finnSluttårsak(sagtOppSelv) shouldBe Sluttårsak.SAGT_OPP_SELV
        Arbeidsforhold.finnSluttårsak(ikkeEndret) shouldBe Sluttårsak.IKKE_ENDRET
    }
}

//language=JSON
val permittertFraFiskeri =
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
val permittert =
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
val avskjediget =
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
val kontraktUtgått =
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
val redusertArbeidstid =
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
val sagtOppAvArbeidsgiver =
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
val sagtOppSelv =
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
val ikkeEndret =
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
