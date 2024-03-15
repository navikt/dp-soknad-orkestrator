package no.nav.dagpenger.soknad.orkestrator.søknad

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SøknadMottakTest {
    private val søknadService = mockk<SøknadService>(relaxed = true)
    private val opplysningRepository = mockk<OpplysningRepository>(relaxed = true)
    private val testRapid = TestRapid()

    init {
        SøknadMottak(rapidsConnection = testRapid, søknadService = søknadService, opplysningRepository = opplysningRepository)
    }

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `vi kan motta søknader`() {
        testRapid.sendTestMessage(innsending_ferdigstilt_event)
    }

    @Test
    fun `vi kan ikke motta søknad dersom forventet felt mangler`() {
        shouldThrow<IllegalArgumentException> {
            testRapid.sendTestMessage(innsending_ferdigstilt_event_uten_fakta)
        }
    }
}

private val innsending_ferdigstilt_event =
    //language=json
    """
     {
      "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
      "@event_name": "innsending_ferdigstilt",
      "@opprettet": "2024-02-21T11:00:27.899791748",
      "journalpostId": "637582711",
      "type": "NySøknad",
      "fødselsnummer": "12345678903",
      "søknadsData": {
        "søknad_uuid": "123e4567-e89b-12d3-a456-426614174000",
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
    """.trimIndent()

private val innsending_ferdigstilt_event_uten_fakta =
    //language=json
    """
     {
      "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
      "@event_name": "innsending_ferdigstilt",
      "@opprettet": "2024-02-21T11:00:27.899791748",
      "journalpostId": "637582711",
      "type": "NySøknad",
      "fødselsnummer": "12345678903",
      "søknadsData": {
        "søknad_uuid": "123e4567-e89b-12d3-a456-426614174000",
        "@opprettet": "2024-02-21T11:00:27.899791748",
        "seksjoner": [
        {
        "beskrivendeId": "bostedsland"
        } 
        ]
      }
    }
    """.trimIndent()
