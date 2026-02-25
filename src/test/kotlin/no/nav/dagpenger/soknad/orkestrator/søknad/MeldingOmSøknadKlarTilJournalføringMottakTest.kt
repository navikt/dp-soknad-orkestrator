package no.nav.dagpenger.soknad.orkestrator.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand.INNSENDT
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.melding.MeldingOmSøknadKlarTilJournalføringMottak
import no.nav.dagpenger.soknad.orkestrator.søknad.pdf.PdfPayloadService
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class MeldingOmSøknadKlarTilJournalføringMottakTest {
    private val søknadId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    private val innsendtTidspunkt = LocalDateTime.parse("2024-02-21T11:00:27.899791748")
    private val ident = "12345678903"
    private val rapidsConnection = TestRapid()
    private val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    private val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    private val pdfPayloadService = mockk<PdfPayloadService>(relaxed = true)

    init {
        MeldingOmSøknadKlarTilJournalføringMottak(
            rapidsConnection,
            søknadRepository,
            seksjonRepository,
            pdfPayloadService,
        )
    }

    @BeforeEach
    fun setUp() {
        rapidsConnection.reset()
    }

    @Test
    fun `onPacket leser melding og behandler den som forventet`() {
        coEvery { søknadRepository.hent(any()) } returns Søknad(søknadId, ident) andThen (Søknad(søknadId, ident, INNSENDT))

        coEvery { seksjonRepository.hentDokumentasjonskrav(any(), any()) } returns dokumentasjonskrav

        rapidsConnection.sendTestMessage(søknadKlarTilJournalføringEvent)

        verify { søknadRepository.markerSøknadSomInnsendt(søknadId, ident, innsendtTidspunkt) }
        verify { pdfPayloadService.genererBruttoPdfPayload(søknadId, ident) }
        verify { pdfPayloadService.genererNettoPdfPayload(søknadId, ident) }
        rapidsConnection.inspektør.size shouldBe 3
        rapidsConnection.inspektør.message(0)["@behov"][0].asText() shouldBe "generer_og_mellomlagre_søknad_pdf"
        rapidsConnection.inspektør.message(1)["@event_name"].asText() shouldBe "søknad_endret_tilstand"
        rapidsConnection.inspektør.message(2)["@event_name"].asText() shouldBe "dokumentkrav_innsendt"
        rapidsConnection.inspektør.message(2)["innsendingsType"].asText() shouldBe "INNSENDT"
        rapidsConnection.inspektør.message(2)["ferdigBesvart"].asText() shouldBe "true"

        val dokumenstasjonskrav = rapidsConnection.inspektør.message(2)["dokumentkrav"]
        dokumenstasjonskrav.size() shouldBe 5
        dokumenstasjonskrav[0]["valg"].asText() shouldBe "SEND_SENERE"
        dokumenstasjonskrav[1]["valg"].asText() shouldBe "SEND_TIDLIGERE"
        dokumenstasjonskrav[2]["valg"].asText() shouldBe "SENDER_IKKE"
        dokumenstasjonskrav[3]["valg"].asText() shouldBe "SEND_NÅ"
        dokumenstasjonskrav[4]["valg"].asText() shouldBe "ETTERSENDT"
    }

    @Test
    fun `onPacket behandler ikke melding hvis søknad har tilstand ulik PÅBEGYNT`() {
        coEvery { søknadRepository.hent(any()) } returns Søknad(søknadId, ident, INNSENDT)

        rapidsConnection.sendTestMessage(søknadKlarTilJournalføringEvent)

        verify(exactly = 0) { søknadRepository.markerSøknadSomInnsendt(søknadId, ident, innsendtTidspunkt) }
        rapidsConnection.inspektør.size shouldBe 0
    }

    @Test
    fun `onPacket behandler ikke melding hvis søknadId ikke eksisterer`() {
        coEvery { søknadRepository.hent(any()) } returns null

        rapidsConnection.sendTestMessage(søknadKlarTilJournalføringEvent)

        verify(exactly = 0) { søknadRepository.markerSøknadSomInnsendt(søknadId, ident, innsendtTidspunkt) }
        rapidsConnection.inspektør.size shouldBe 0
    }

    @Test
    fun `onPacket behandler ikke melding hvis innsendt søknadId har en annen ident enn lagret søknadId`() {
        coEvery { søknadRepository.hent(any()) } returns Søknad(søknadId, "en_annen_ident")

        rapidsConnection.sendTestMessage(søknadKlarTilJournalføringEvent)

        verify(exactly = 0) { søknadRepository.markerSøknadSomInnsendt(søknadId, ident, innsendtTidspunkt) }
        rapidsConnection.inspektør.size shouldBe 0
    }

    private val søknadKlarTilJournalføringEvent =
        //language=json
        """
        {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "@event_name": "${MeldingOmSøknadKlarTilJournalføringMottak.EVENT_NAME}",
          "@opprettet": "2024-02-21T11:00:27.899791748",
          "ident": "$ident",
          "søknadId": "$søknadId",
          "innsendtTidspunkt": "$innsendtTidspunkt"
        }
        """.trimIndent()

    private val dokumentasjonskrav =
        listOf(
            """
            [
                {
                  "id": "b8106828-cee3-4634-938a-787fbb828dbb",
                  "seksjonId": "arbeidsforhold",
                  "spørsmålId": "hvordanHarDetteArbeidsforholdetEndretSeg",
                  "skjemakode": "T8",
                  "tittel": "Oppsigelse - Bedrift 1 AS",
                  "type": "ArbeidsforholdArbeidsgiverenMinHarSagtMegOpp",
                  "svar": "dokumentkravSvarSenderSenere",
                  "begrunnelse": "rrr"
                },
                {
                  "id": "ad70b80b-0d24-445d-87d4-7d08dd68d355",
                  "seksjonId": "arbeidsforhold",
                  "spørsmålId": "hvordanHarDetteArbeidsforholdetEndretSeg",
                  "skjemakode": "O2",
                  "tittel": "Arbeidsavtale - Bedrift 2 DA",
                  "type": "ArbeidsforholdArbeidsavtale",
                  "svar": "dokumentkravSvarSendtTidligere",
                  "begrunnelse": "bbb?"
                },
                {
                  "id": "5fd32960-4b66-455f-a5e0-df49d8cde9e1",
                  "seksjonId": "arbeidsforhold",
                  "spørsmålId": "hvordanHarDetteArbeidsforholdetEndretSeg",
                  "skjemakode": "T6",
                  "tittel": "Permitteringsvarsel - Bedrift 2 DA",
                  "type": "ArbeidsforholdPermitteringsvarsel",
                  "svar": "dokumentkravSvarSenderIkke",
                  "begrunnelse": "ccc"
                }
                          ]
            """.trimIndent(),
            """
            [
              {
                "id": "93e3d573-6de7-4ba5-96d6-805921eaacf1",
                "seksjonId": "arbeidsforhold",
                "spørsmålId": "hvordanHarDetteArbeidsforholdetEndretSeg",
                "skjemakode": "O2",
                "tittel": "Arbeidsavtale - ddd",
                "type": "ArbeidsforholdArbeidsavtale",
                "svar": "dokumentkravSvarSendNå",
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
              },
              {
                "id": "a67bce29-eb2a-4c1d-8433-69ac9fd63a33",
                "seksjonId": "arbeidsforhold",
                "spørsmålId": "hvordanHarDetteArbeidsforholdetEndretSeg",
                "skjemakode": "T8",
                "tittel": "Oppsigelse - ddd",
                "type": "ArbeidsforholdArbeidsgiverenMinHarSagtMegOpp",
                "svar": "dokumentkravEttersendt",
                "filer": [
                  {
                    "id": "e9ff1647-aea6-4be3-9414-5a66c4ea29ef",
                    "file": {},
                    "filnavn": "oppsigelse.png",
                    "lasterOpp": false,
                    "urn": "urn:vedlegg:3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/a67bce29-eb2a-4c1d-8433-69ac9fd63a33/8bebe9b5-503f-48a1-aaf9-80ea1c8f4685",
                    "filsti": "3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/a67bce29-eb2a-4c1d-8433-69ac9fd63a33/8bebe9b5-503f-48a1-aaf9-80ea1c8f4685",
                    "storrelse": 4626,
                    "tidspunkt": "2026-02-19T08:38:52.532296475+01:00"
                  }
                ],
                "bundle": {
                  "filnavn": "a67bce29-eb2a-4c1d-8433-69ac9fd63a33",
                  "urn": "urn:vedlegg:3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/7820f4b0-cc66-4ac1-a6ae-eef1255102ea",
                  "filsti": "3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/7820f4b0-cc66-4ac1-a6ae-eef1255102ea",
                  "storrelse": 150602,
                  "tidspunkt": "2026-02-19T08:38:55.90602091+01:00"
                }
              }
            ]
            """.trimIndent(),
        )
}
