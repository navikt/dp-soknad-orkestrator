package no.nav.dagpenger.soknad.orkestrator.barn

import io.ktor.http.HttpHeaders
import mu.KotlinLogging
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.pdl.PersonOppslagBolk
import no.nav.dagpenger.pdl.adresse.AdresseMapper
import no.nav.dagpenger.pdl.adresse.AdresseVisitor
import no.nav.dagpenger.pdl.adresse.PDLAdresse
import no.nav.dagpenger.soknad.orkestrator.barn.BarnService.Iso3LandkodeMapper.UKJENT

class BarnService(
    private val personOppslagBolk: PersonOppslagBolk,
    private val tokenProvider: () -> String,
) {
    private val sikkerLogg = KotlinLogging.logger("tjenestekall")

    suspend fun hentBarn(fnr: String): List<BarnDto> =
        try {
            personOppslagBolk
                .hentBarn(
                    fnr,
                    mapOf(
                        HttpHeaders.Authorization to "Bearer ${tokenProvider.invoke()}",
                        "behandlingsnummer" to "B286",
                    ),
                ).map { mapToBarn(it) }
                .filter { it.alder() < 18 }
        } catch (e: Exception) {
            sikkerLogg.error(e) { "Feil under utehenting av barn fra PDL for fnr $fnr." }
            throw e
        }

    internal fun mapToBarn(pdlPerson: PDLPerson): BarnDto =
        try {
            BarnDto(
                fornavn = pdlPerson.fornavn,
                mellomnavn = pdlPerson.mellomnavn ?: "",
                etternavn = pdlPerson.etternavn,
                fodselsdato = pdlPerson.fodselsdato,
                bostedsland =
                    AdresseVisitor(pdlPerson).bostedsadresse?.let { bostedsAdresse ->
                        Iso3LandkodeMapper.formatertAdresse(
                            bostedsAdresse,
                        )
                    } ?: UKJENT,
                hentetFraPdl = true,
            )
        } catch (e: PDLPerson.PDLException) {
            if (e.message == "Ingen fodselsnummer funnet") {
                sikkerLogg.warn(e) { "Fant barn uten fødselsnummer." }
            }
            throw e
        }

    internal object Iso3LandkodeMapper : AdresseMapper<String>() {
        const val NORGE = "NOR"
        const val UKJENT = "XUK"

        private fun String?.landkode() = this ?: UKJENT

        override fun formatertAdresse(pdlAdresse: PDLAdresse.MatrikkelAdresse) = NORGE

        override fun formatertAdresse(pdlAdresse: PDLAdresse.PostAdresseIFrittFormat) = NORGE

        override fun formatertAdresse(pdlAdresse: PDLAdresse.PostboksAdresse) = NORGE

        override fun formatertAdresse(pdlAdresse: PDLAdresse.TomAdresse) = UKJENT

        override fun formatertAdresse(pdlAdresse: PDLAdresse.UtenlandsAdresseIFrittFormat) = pdlAdresse.landKode.landkode()

        override fun formatertAdresse(pdlAdresse: PDLAdresse.UtenlandskAdresse) = pdlAdresse.landKode.landkode()

        override fun formatertAdresse(pdlAdresse: PDLAdresse.VegAdresse) = NORGE
    }
}
