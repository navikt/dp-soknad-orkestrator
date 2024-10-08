package no.nav.dagpenger.soknad.orkestrator.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SøknadMottakTest {
    private val testRapid = TestRapid()
    private val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    private val søknadService =
        SøknadService(
            søknadRepository = søknadRepository,
            opplysningRepository = mockk<OpplysningRepository>(relaxed = true),
        ).also {
            it.setRapidsConnection(testRapid)
        }

    init {
        SøknadMottak(
            rapidsConnection = testRapid,
            søknadService = søknadService,
            søknadRepository = søknadRepository,
        )
    }

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `vi kan motta søknader`() {
        testRapid.sendTestMessage(søknad_innsendt_event)
        testRapid.inspektør.size shouldBe 1
    }

    @Test
    fun `vi mottar ikke meldinger som mangler påkrevde felter`() {
        testRapid.sendTestMessage(søknad_innsendt_event_uten_ident)
        testRapid.inspektør.size shouldBe 0

        testRapid.sendTestMessage(søknad_innsendt_event_uten_søknadId)
        testRapid.inspektør.size shouldBe 0

        testRapid.sendTestMessage(søknad_innsendt_event_uten_søknadstidspunkt)
        testRapid.inspektør.size shouldBe 0

        testRapid.sendTestMessage(søknad_innsendt_event_uten_søknadData)
        testRapid.inspektør.size shouldBe 0
    }
}

private val søknad_innsendt_event =
    //language=json
    """
    {
      "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
      "@event_name": "søknad_innsendt_varsel",
      "@opprettet": "2024-02-21T11:00:27.899791748",
      "søknadId": "123e4567-e89b-12d3-a456-426614174000",
      "ident": "12345678903",
      "søknadstidspunkt": "2024-02-21T11:00:27.899791748",
      "søknadData": {
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

private val søknad_innsendt_event_uten_ident =
    //language=json
    """
    {
      "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
      "@event_name": "søknad_innsendt_varsel",
      "@opprettet": "2024-02-21T11:00:27.899791748",
      "søknadId": "123e4567-e89b-12d3-a456-426614174000",
      "søknadstidspunkt": "2024-02-21T11:00:27.899791748",
      "søknadData": {
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

private val søknad_innsendt_event_uten_søknadId =
    //language=json
    """
    {
      "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
      "@event_name": "søknad_innsendt_varsel",
      "@opprettet": "2024-02-21T11:00:27.899791748",
      "ident": "12345678903",    
      "søknadstidspunkt": "2024-02-21T11:00:27.899791748",
      "søknadData": {
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

private val søknad_innsendt_event_uten_søknadstidspunkt =
    //language=json
    """
    {
      "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
      "@event_name": "søknad_innsendt_varsel",
      "@opprettet": "2024-02-21T11:00:27.899791748",
      "søknadId": "123e4567-e89b-12d3-a456-426614174000",
      "ident": "12345678903",    
      "søknadData": {
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

private val søknad_innsendt_event_uten_søknadData =
    //language=json
    """
    {
      "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
      "@event_name": "søknad_innsendt_varsel",
      "@opprettet": "2024-02-21T11:00:27.899791748",
      "søknadId": "123e4567-e89b-12d3-a456-426614174000",
      "ident": "12345678903",    
      "søknadstidspunkt": "2024-02-21T11:00:27.899791748"
    }
    """.trimIndent()
