package no.nav.dagpenger.soknad.orkestrator.opplysning

import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import java.time.LocalDate
import java.util.UUID

class OpplysningService(val opplysningRepository: QuizOpplysningRepository) {
    fun hentBarn(): List<BarnDTO> {
        return listOf(
            BarnDTO(
                barnSvarId = UUID.randomUUID(),
                fornavnOgMellomnavn = "Ola",
                etternavn = "Nordmann",
                fødselsdato = LocalDate.of(2010, 1, 1),
                oppholdssted = "NOR",
                forsørgerBarnet = true,
                fraRegister = true,
                girBarnetillegg = true,
                girBarnetilleggFom = LocalDate.of(2010, 1, 1),
                girBarnetilleggTom = LocalDate.of(2028, 1, 1),
            ),
            BarnDTO(
                barnSvarId = UUID.randomUUID(),
                fornavnOgMellomnavn = "Kari",
                etternavn = "Nordmann",
                fødselsdato = LocalDate.of(2012, 1, 1),
                oppholdssted = "NOR",
                forsørgerBarnet = true,
                fraRegister = true,
                girBarnetillegg = true,
                girBarnetilleggFom = LocalDate.of(2012, 1, 1),
                girBarnetilleggTom = LocalDate.of(2030, 1, 1),
            ),
        )
    }
}
