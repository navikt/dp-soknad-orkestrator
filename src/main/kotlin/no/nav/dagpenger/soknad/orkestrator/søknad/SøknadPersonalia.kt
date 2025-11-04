package no.nav.dagpenger.soknad.orkestrator.søknad

import java.util.UUID

data class SøknadPersonalia(
    val søknadId: UUID,
    val ident: String,
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
    val alder: String,
    val adresselinje1: String? = null,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val landkode: String? = null,
    val land: String? = null,
    val kontonummer: String? = null,
)
