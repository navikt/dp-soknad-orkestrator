package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.behov.SøknadBehovmelding
import java.util.UUID

fun lagBehovmelding(
    ident: String,
    søknadId: UUID,
    behov: BehovløserFactory.Behov,
): Behovmelding =
    Behovmelding(
        JsonMessage
            .newMessage(
                eventName = "behov",
                map =
                    mapOf(
                        "ident" to ident,
                        "søknadId" to søknadId,
                        "@behov" to listOf(behov),
                        "journalpostId" to "12345679",
                    ),
            ).apply { this.requireKey("ident", "søknadId", "@behov", "journalpostId") },
    )

fun lagBehovmeldingUtenSøknadId(ident: String): SøknadBehovmelding =
    SøknadBehovmelding(
        JsonMessage
            .newMessage(
                eventName = "behov",
                map =
                    mapOf(
                        "ident" to ident,
                        "@behov" to listOf("søknadsdata"),
                        "journalpostId" to "12345679",
                    ),
            ).apply { this.requireKey("ident", "@behov", "journalpostId") },
    )
