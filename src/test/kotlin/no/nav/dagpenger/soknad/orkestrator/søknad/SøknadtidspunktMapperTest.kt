package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.utils.januar
import org.junit.Test
import java.time.ZonedDateTime
import java.util.UUID

private val søknadId = UUID.randomUUID()
private val ident = "12345678903"
private val søknadstidspunkt = ZonedDateTime.now().toString()

class SøknadtidspunktMapperTest {
    @Test
    fun `søknad_innsendt event med søknadstidspunkt mappes riktig`() {
        val tidspunktOpplysning = SøknadtidspunktMapper(søknad_innsendt_event).tidspunktOpplysning

        tidspunktOpplysning shouldBe Opplysning("søknadstidspunkt", Tekst, søknadstidspunkt, ident, søknadId)
    }
}

private val søknad_innsendt_event =
    //language=json
    ObjectMapper().readTree(
        """
        {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "@event_name": "søknad_innsendt_varsel",
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
                    "svar": "${1.januar}",
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
