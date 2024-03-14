package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SøknadMapperTest {
    @Test
    fun `vi kan ikke mappe dersom søknad_uuid mangler`() {
        shouldThrow<IllegalArgumentException> {
            SøknadMapper(søknadsDataUtenSøknadUUID).søknad
        }
    }

    @Test
    fun `vi kan ikke mappe dersom seksjoner mangler`() {
        shouldThrow<IllegalArgumentException> {
            SøknadMapper(søknadsDataUtenSeksjoner).søknad
        }
    }

    @Test
    fun `vi kan ikke mappe dersom fakta mangler`() {
        shouldThrow<IllegalArgumentException> {
            SøknadMapper(søknadsDataUtenFakta).søknad
        }
    }

    @Test
    fun `svaret på generatorfaktum settes til tom string`() {
        val søknad = SøknadMapper(søknadsDataMedPeriodeFaktum).søknad
        søknad.opplysninger.first().svar shouldBe ""
    }
}

private val søknadsDataUtenSøknadUUID =
    ObjectMapper().readTree(
        //language=json
        """
         {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "fødselsnummer": "12345678903",
          "søknadsData": {
            "@opprettet": "2024-02-21T11:00:27.899791748",
            "seksjoner": [
            {
              "fakta": [
              {
                "id": "6001",
                "svar": "NOR",
                "type": "land",
                "beskrivendeId": "faktum.hvilket-land-bor-du-i"
              } 
              ],
            "beskrivendeId": "bostedsland"
            } 
            ]
          }
        }
        """.trimIndent(),
    )

private val søknadsDataUtenSeksjoner =
    ObjectMapper().readTree(
        //language=json
        """
         {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "fødselsnummer": "12345678903",
          "søknadsData": {
            "@opprettet": "2024-02-21T11:00:27.899791748",
            "søknad_uuid": "123e4567-e89b-12d3-a456-426614174000"
          }
        }
        """.trimIndent(),
    )

private val søknadsDataUtenFakta =
    ObjectMapper().readTree(
        //language=json
        """
         {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "fødselsnummer": "12345678903",
          "søknadsData": {
            "@opprettet": "2024-02-21T11:00:27.899791748",
            "søknad_uuid": "123e4567-e89b-12d3-a456-426614174000",
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
          "fødselsnummer": "12345678903",
          "søknadsData": {
            "@opprettet": "2024-02-21T11:00:27.899791748",
            "søknad_uuid": "123e4567-e89b-12d3-a456-426614174000",
            "seksjoner": [
            {
              "fakta": [
              {
                "id": "6001",
                "svar": {"fom": "2024-01-01", "tom": "2024-02-01"},
                "type": "periode",
                "beskrivendeId": "faktum.hvilket-land-bor-du-i"
              } 
              ],
            "beskrivendeId": "bostedsland"
            } 
            ]
          }
        }
        """.trimIndent(),
    )
