package no.nav.dagpenger.soknad.orkestrator.søknad.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.mottak.MeldingOmEttersendingMottak.Companion.BEHOV
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MeldingOmEttersendingMottakTest {
    private val søknadId = UUID.randomUUID()
    private val ident = "12345678901"
    private val journalpostId = "12316461"
    private val journalførtTidspunkt = now()
    private val seksjonId = "barnetillegg" as String
    private val seksjonData = dokumentasjonskravJson()
    private val seksjonDataMini = seksjonData.replace("\n", "").replace(" ", "")
    private val rapidsConnection = TestRapid()
    private val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    private val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)

    init {
        MeldingOmEttersendingMottak(rapidsConnection, søknadRepository, seksjonRepository)
    }

    @Test
    fun `onPacket leser melding og behandler som forventet`() {
        coEvery { søknadRepository.hent(søknadId) } returns
            Søknad(
                ident = ident,
                søknadId = søknadId,
                tilstand = Tilstand.JOURNALFØRT,
            )

        rapidsConnection.sendTestMessage(journalførSøknadPdfOgVedleggMelding())
        verify {
            seksjonRepository.lagreDokumentasjonskravEttersending(
                søknadId,
                ident,
                seksjonId,
                seksjonDataMini,
                any(LocalDateTime::class),
            )
        }

        rapidsConnection.inspektør.size shouldBe 1
        val dokumentKravInnsendtMelding = rapidsConnection.inspektør.message(0)

        dokumentKravInnsendtMelding["@event_name"].asText() shouldBe "dokumentkrav_innsendt"
        dokumentKravInnsendtMelding["ident"].asText() shouldBe ident
        dokumentKravInnsendtMelding["søknad_uuid"].asText() shouldBe søknadId.toString()
        dokumentKravInnsendtMelding["innsendingsType"].asText() shouldBe "ETTERSENDT"
        dokumentKravInnsendtMelding["ferdigBesvart"].asText() shouldBe "true"
        dokumentKravInnsendtMelding["kilde"].asText() shouldBe "orkestrator"
        dokumentKravInnsendtMelding["dokumentkrav"].size() shouldBe 1
        dokumentKravInnsendtMelding["dokumentkrav"][0]["dokumentnavn"].asText() shouldBe "ArbeidsforholdArbeidsavtale"
        dokumentKravInnsendtMelding["dokumentkrav"][0]["skjemakode"].asText() shouldBe "O2"
        dokumentKravInnsendtMelding["dokumentkrav"][0]["valg"].asText() shouldBe "ETTERSENDT"
    }

    @Test
    fun `onPacket leser melding og behandler med ulik ident skal feile`() {
        coEvery { søknadRepository.hent(søknadId) } returns
            Søknad(
                ident = "1234567",
                søknadId = søknadId,
                tilstand = Tilstand.INNSENDT,
            )

        assertFailsWith<IllegalArgumentException> {
            rapidsConnection.sendTestMessage(journalførSøknadPdfOgVedleggMelding())
        }

        verify(exactly = 0) {
            seksjonRepository.lagreDokumentasjonskravEttersending(
                søknadId,
                ident,
                seksjonId,
                seksjonDataMini,
                any(LocalDateTime::class),
            )
        }

        rapidsConnection.inspektør.size shouldBe 0
    }

    private fun journalførSøknadPdfOgVedleggMelding(): String {
        val escapedSeksjonData = seksjonData.replace("\"", "\\\"").replace("\n", "").replace(" ", "")
        return """
            {
              "@id": "72847f73-23e3-489d-940d-fc0a01a62235",
              "@event_name": "behov",
              "@behov": ["journalfør_ettersending_av_dokumentasjon"],
              "@opprettet": "2025-01-26T15:30:27.899791748",
              "@final": true,
              "@løsning": {
                "$BEHOV" :  {
                   "journalpostId": "$journalpostId", 
                   "journalførtTidspunkt": "$journalførtTidspunkt",
                   "dokumentasjonskravJson": "$escapedSeksjonData",
                   "seksjonId": "$seksjonId"
                }
              },
              "ident": "$ident",
              "søknadId": "$søknadId"
            }
            """.trimIndent()
    }

    private fun dokumentasjonskravJson() =
        """
        [
            {
              "id": "93e3d573-6de7-4ba5-96d6-805921eaacf1",
              "seksjonId": "arbeidsforhold",
              "spørsmålId": "hvordanHarDetteArbeidsforholdetEndretSeg",
              "skjemakode": "O2",
              "tittel": "Arbeidsavtale - ddd",
              "type": "ArbeidsforholdArbeidsavtale",
              "svar": "dokumentkravEttersendt",
              "filer": [
                {
                  "id": "24efe9de-5380-493b-97da-766096975d1e",
                  "file": {},
                  "filnavn": "sol.png",
                  "lasterOpp": false,
                  "urn": "urn:vedlegg:3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/93e3d573-6de7-4ba5-96d6-805921eaacf1/66eefd6e-2f31-424b-98f4-cd44b9f934ee",
                  "filsti": "3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/93e3d573-6de7-4ba5-96d6-805921eaacf1/66eefd6e-2f31-424b-98f4-cd44b9f934ee",
                  "storrelse": 1879,
                  "tidspunkt": "2026-02-19T08:32:25.550864975+01:00"
                }
              ],
              "bundle": {
                "filnavn": "93e3d573-6de7-4ba5-96d6-805921eaacf1",
                "urn": "urn:vedlegg:3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/43232246-0bdd-4bd3-9161-34d7217f8496",
                "filsti": "3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/43232246-0bdd-4bd3-9161-34d7217f8496",
                "storrelse": 86085,
                "tidspunkt": "2026-02-19T08:32:29.331083379+01:00"
              }
            }
        ]
        """.trimIndent()
}
