package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.contain
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.utils.januar
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID

private val søknadId = UUID.randomUUID()
private val ident = "12345678903"
private val søknadstidspunkt = ZonedDateTime.now().toString()
private val ønskerDagpengerFra = 1.januar.toString()

class SøknadMapperTest {
    @Test
    fun `vi kan mappe søknad_innsendt event riktig`() {
        val søknad = SøknadMapper(søknad_innsendt_event).søknad
        søknad.id shouldBe søknadId
        søknad.ident shouldBe ident
        søknad.opplysninger.size shouldBe 3

        søknad.opplysninger should {
            contain(
                Opplysning(
                    "faktum.mottatt-dagpenger-siste-12-mnd",
                    "faktum.mottatt-dagpenger-siste-12-mnd.svar.nei",
                    ident,
                    søknadId,
                ),
            )
            contain(Opplysning("faktum.dagpenger-soknadsdato", ønskerDagpengerFra, ident, søknadId))
            contain(Opplysning("søknadstidspunkt", søknadstidspunkt, ident, søknadId))
        }
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

    // TODO - Fjern når vi har håndtert periodefaktum
    @Test
    fun `svaret på periodefaktum settes til tom string`() {
        val søknad = SøknadMapper(søknadsDataMedPeriodeFaktum).søknad
        søknad.opplysninger.first().svar shouldBe ""
    }

    // TODO - Fjern når vi har håndtert generatorfaktum
    @Test
    fun `svaret på generatorfaktum settes til tom string`() {
        val søknad = SøknadMapper(søknadsDataMedGeneratorFaktum).søknad
        søknad.opplysninger.first().svar shouldBe ""
    }
}

private val søknad_innsendt_event =
    //language=json
    ObjectMapper().readTree(
        """
        {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "@event_name": "søknad_innsendt",
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
          "@event_name": "søknad_innsendt",
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
          "@event_name": "søknad_innsendt",
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
          "@event_name": "søknad_innsendt",
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

private val søknadsDataMedGeneratorFaktum =
    ObjectMapper().readTree(
        //language=json
        """
        {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "@event_name": "søknad_innsendt",
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
