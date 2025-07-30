package no.nav.dagpenger.soknad.orkestrator.personalia

import no.nav.dagpenger.pdl.adresse.AdresseMetadata
import no.nav.dagpenger.pdl.adresse.PDLAdresse
import no.nav.dagpenger.pdl.adresse.PostAdresseOrder
import no.nav.dagpenger.soknad.orkestrator.personalia.PDLAdresseMapper.LandDataOppslag.finnLand
import no.nav.dagpenger.soknad.orkestrator.personalia.PDLAdresseMapper.PostDataOppslag.finnPoststed
import no.nav.dagpenger.soknad.orkestrator.personalia.PDLAdresseMapper.formatertAdresse
import no.nav.pam.geography.Country
import no.nav.pam.geography.CountryDAO
import no.nav.pam.geography.PostDataDAO
import java.io.IOException
import kotlin.collections.filterNot
import kotlin.collections.firstOrNull
import kotlin.collections.getOrNull
import kotlin.collections.joinToString
import kotlin.collections.sortedWith

class AdresseMapper(
    pdlAdresser: List<PDLAdresse>,
) {
    val folkeregistertAdresse: AdresseDto?
    val postAdresse: AdresseDto?

    init {
        val sortert = pdlAdresser.sortedWith(PostAdresseOrder.comparator)
        folkeregistertAdresse =
            sortert
                .firstOrNull { it.adresseMetadata.adresseType == AdresseMetadata.AdresseType.BOSTEDSADRESSE }
                ?.let(::formatertAdresse)

        postAdresse = sortert.firstOrNull()?.let(::formatertAdresse)
    }
}

@Suppress("DuplicatedCode")
internal object PDLAdresseMapper : no.nav.dagpenger.pdl.adresse.AdresseMapper<AdresseDto>() {
    private class GeografiOppslagInitException(
        e: Exception,
    ) : RuntimeException(e)

    private object PostDataOppslag {
        private val dao: PostDataDAO =
            try {
                PostDataDAO()
            } catch (e: IOException) {
                throw GeografiOppslagInitException(e)
            }

        fun finnPoststed(postNummer: String?): String? =
            postNummer?.let {
                dao.findPostData(postNummer).map { it.capitalizedCityName }.orElse(null)
            }
    }

    private object LandDataOppslag {
        private val dao: CountryDAO =
            try {
                CountryDAO()
            } catch (e: IOException) {
                throw GeografiOppslagInitException(e)
            }

        fun finnLand(landKode: String?): Country? = landKode?.let { dao.findCountryByCode(it).orElse(null) }
    }

    override fun formatertAdresse(pdlAdresse: PDLAdresse.MatrikkelAdresse): AdresseDto = AdresseDto()

    override fun formatertAdresse(pdlAdresse: PDLAdresse.PostAdresseIFrittFormat): AdresseDto {
        with(pdlAdresse) {
            val adresseLinjer = listOf(adresseLinje1, adresseLinje2, adresseLinje3).filterNot(String?::isNullOrBlank)

            return AdresseDto(
                adresselinje1 = adresseLinjer.getOrNull(0) ?: "",
                adresselinje2 = adresseLinjer.getOrNull(1) ?: "",
                adresselinje3 = adresseLinjer.getOrNull(2) ?: "",
                postnummer = postnummer ?: "",
                poststed = finnPoststed(postnummer) ?: "",
                landkode = "NO",
                land = "NORGE",
            )
        }
    }

    override fun formatertAdresse(pdlAdresse: PDLAdresse.PostboksAdresse): AdresseDto {
        with(pdlAdresse) {
            val adresseLinjer = listOf(postbokseier, postboks).filterNot(String?::isNullOrBlank)
            return AdresseDto(
                adresselinje1 = adresseLinjer.getOrNull(0) ?: "",
                adresselinje2 = adresseLinjer.getOrNull(1) ?: "",
                adresselinje3 = "",
                postnummer = postnummer ?: "",
                poststed = finnPoststed(postnummer) ?: "",
                landkode = "NO",
                land = "NORGE",
            )
        }
    }

    override fun formatertAdresse(pdlAdresse: PDLAdresse.TomAdresse): AdresseDto = AdresseDto()

    override fun formatertAdresse(pdlAdresse: PDLAdresse.UtenlandsAdresseIFrittFormat): AdresseDto {
        with(pdlAdresse) {
            val adresseLinjer = listOf(adresseLinje1, adresseLinje2, adresseLinje3).filterNot(String?::isNullOrBlank)

            val land = finnLand(landKode)
            return AdresseDto(
                adresselinje1 = adresseLinjer.getOrNull(0) ?: "",
                adresselinje2 = adresseLinjer.getOrNull(1) ?: "",
                adresselinje3 = adresseLinjer.getOrNull(2) ?: "",
                postnummer = postkode ?: "",
                poststed = byEllerStedsnavn ?: "",
                landkode = land?.alpha2Code ?: "",
                land = land?.name ?: "",
            )
        }
    }

    override fun formatertAdresse(pdlAdresse: PDLAdresse.UtenlandskAdresse): AdresseDto {
        with(pdlAdresse) {
            val adresseLinjer =
                listOf(adressenavnNummer, bygningEtasjeLeilighet, postboksNummerNavn).filterNot(String?::isNullOrBlank)

            val land = finnLand(landKode)
            return AdresseDto(
                adresselinje1 = adresseLinjer.getOrNull(0) ?: "",
                adresselinje2 = adresseLinjer.getOrNull(1) ?: "",
                adresselinje3 = adresseLinjer.getOrNull(2) ?: "",
                postnummer = postkode ?: "",
                poststed = bySted ?: "",
                landkode = land?.alpha2Code ?: "",
                land = land?.name ?: "",
            )
        }
    }

    override fun formatertAdresse(pdlAdresse: PDLAdresse.VegAdresse): AdresseDto {
        val husNummerBokstav =
            listOf(pdlAdresse.husnummer, pdlAdresse.husbokstav).filterNot(String?::isNullOrBlank).joinToString("")

        val adresselinje2 =
            listOf(pdlAdresse.adressenavn, husNummerBokstav)
                .filterNot(String?::isNullOrBlank)
                .joinToString(separator = " ")

        val adresseLinjer = listOf(pdlAdresse.adresseMetadata.coAdresseNavn, adresselinje2).filterNot(String?::isNullOrBlank)

        return AdresseDto(
            adresselinje1 = adresseLinjer.getOrNull(0) ?: "",
            adresselinje2 = adresseLinjer.getOrNull(1) ?: "",
            postnummer = pdlAdresse.postnummer ?: "",
            poststed = finnPoststed(pdlAdresse.postnummer) ?: "",
            landkode = "NO",
            land = "NORGE",
        )
    }
}
