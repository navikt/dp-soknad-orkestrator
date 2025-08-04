package no.nav.dagpenger.soknad.orkestrator.utils

import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.pdl.PDLPerson
import java.time.LocalDate

class PdlTestUtil {
    fun lagPdlPerson(
        fornavn: String,
        mellomnavn: String,
        etternavn: String,
        fodselsdato: LocalDate,
        fodseslnummer: String,
    ): PDLPerson {
        val pdlPerson = mockk<PDLPerson>(relaxed = true)
        every { pdlPerson.fornavn } returns fornavn
        every { pdlPerson.mellomnavn } returns mellomnavn
        every { pdlPerson.etternavn } returns etternavn
        every { pdlPerson.fodselsdato } returns fodselsdato
        every { pdlPerson.fodselnummer } returns fodseslnummer
        return pdlPerson
    }
}
