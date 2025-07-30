package no.nav.dagpenger.soknad.orkestrator.personalia

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.pdl.adresse.AdresseMetadata
import no.nav.dagpenger.pdl.adresse.AdresseMetadata.AdresseType
import no.nav.dagpenger.pdl.adresse.AdresseMetadata.AdresseType.BOSTEDSADRESSE
import no.nav.dagpenger.pdl.adresse.AdresseMetadata.AdresseType.KONTAKTADRESSE
import no.nav.dagpenger.pdl.adresse.AdresseMetadata.AdresseType.OPPHOLDSADRESSE
import no.nav.dagpenger.pdl.adresse.AdresseMetadata.MasterType
import no.nav.dagpenger.pdl.adresse.PDLAdresse
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AdresseMapperTest {
    @Test
    fun `AdresseMapper bruker BOSTEDSADRESSE som folkeregistrert adresse`() {
        val pdlAdresses =
            listOf(
                createPdlAdresse(OPPHOLDSADRESSE, "2000"),
                createPdlAdresse(BOSTEDSADRESSE, "2001"),
                createPdlAdresse(KONTAKTADRESSE, "2002"),
            )

        AdresseMapper(pdlAdresses).folkeregistertAdresse?.postnummer shouldBe "2001"
    }

    @Test
    fun `AdresseMapper returnerer null som folkeregistrert adresse hvis input er tom liste`() {
        AdresseMapper(emptyList()).folkeregistertAdresse shouldBe null
    }

    @Test
    fun `AdresseMapper returnerer null som postadresse adresse hvis input er tom liste`() {
        AdresseMapper(emptyList()).postAdresse shouldBe null
    }

    @Test
    fun `AdresseMapper returnerer null som folkeregistrert adresse hvis input ikke inneholder BOSTEDSADRESSE`() {
        AdresseMapper(
            listOf(
                createPdlAdresse(OPPHOLDSADRESSE, "2000"),
                createPdlAdresse(KONTAKTADRESSE, "2001"),
            ),
        ).folkeregistertAdresse shouldBe null
    }

    @Test
    fun `AdresseMapper bruker KONTAKTADRESSE som postadresse hvis den eksisterer`() {
        val pdlAdresses =
            listOf(
                createPdlAdresse(OPPHOLDSADRESSE, "2000"),
                createPdlAdresse(BOSTEDSADRESSE, "2001"),
                createPdlAdresse(KONTAKTADRESSE, "2002"),
            )

        AdresseMapper(pdlAdresses).postAdresse?.postnummer shouldBe "2002"
    }

    @Test
    fun `AdresseMapper bruker OPPHOLDSADRESSE som postadresse hvis den eksisterer og KONTAKTADRESSE ikke eksisterer`() {
        val pdlAdresses =
            listOf(
                createPdlAdresse(OPPHOLDSADRESSE, "2000"),
                createPdlAdresse(BOSTEDSADRESSE, "2001"),
            )

        AdresseMapper(pdlAdresses).postAdresse?.postnummer shouldBe "2000"
    }

    @Test
    fun `AdresseMapper bruker BOSTEDSADRESSE som postadresse hvis det er den eneste adressen som eksisterer`() {
        val pdlAdresses =
            listOf(
                createPdlAdresse(BOSTEDSADRESSE, "2001"),
            )

        AdresseMapper(pdlAdresses).postAdresse?.postnummer shouldBe "2001"
    }

    @Test
    fun `AdresseMapper mapper PDLAdresse_PostAdresseIFrittFormat til forventede verdier hvis PDL returnerer adresse med verdier`() {
        val pdflAdresse =
            PDLAdresse.PostAdresseIFrittFormat(
                adresseMetadata =
                    AdresseMetadata(
                        adresseType = BOSTEDSADRESSE,
                        master = MasterType.PDL,
                    ),
                adresseLinje1 = "adr1",
                adresseLinje2 = "adr2",
                adresseLinje3 = "adr3",
                postnummer = "1453",
            )

        val adresseMapper = AdresseMapper(listOf(pdflAdresse))

        adresseMapper.postAdresse?.adresselinje1 shouldBe "adr1"
        adresseMapper.postAdresse?.adresselinje2 shouldBe "adr2"
        adresseMapper.postAdresse?.adresselinje3 shouldBe "adr3"
        adresseMapper.postAdresse?.postnummer shouldBe "1453"
        adresseMapper.postAdresse?.poststed shouldBe "Bjørnemyr"
        adresseMapper.postAdresse?.landkode shouldBe "NO"
        adresseMapper.postAdresse?.land shouldBe "NORGE"
    }

    @Test
    fun `AdresseMapper mapper PDLAdresse_PostAdresseIFrittFormat til forventede verdier hvis PDL returnerer adresse uten verdier`() {
        val pdflAdresse =
            PDLAdresse.PostAdresseIFrittFormat(
                adresseMetadata =
                    AdresseMetadata(
                        adresseType = BOSTEDSADRESSE,
                        master = MasterType.PDL,
                    ),
                adresseLinje1 = null,
                adresseLinje2 = null,
                adresseLinje3 = null,
                postnummer = null,
            )

        val adresseMapper = AdresseMapper(listOf(pdflAdresse))

        adresseMapper.postAdresse?.adresselinje1 shouldBe ""
        adresseMapper.postAdresse?.adresselinje2 shouldBe ""
        adresseMapper.postAdresse?.adresselinje3 shouldBe ""
        adresseMapper.postAdresse?.postnummer shouldBe ""
        adresseMapper.postAdresse?.poststed shouldBe ""
        adresseMapper.postAdresse?.landkode shouldBe "NO"
        adresseMapper.postAdresse?.land shouldBe "NORGE"
    }

    @Test
    fun `AdresseMapper mapper PDLAdresse_PostboksAdresse til forventede verdier hvis PDL returnerer adresse med verdier`() {
        val pdflAdresse =
            PDLAdresse.PostboksAdresse(
                adresseMetadata =
                    AdresseMetadata(
                        adresseType = BOSTEDSADRESSE,
                        master = MasterType.PDL,
                    ),
                postbokseier = "postbokseier",
                postboks = "postboks",
                postnummer = "1453",
            )

        val adresseMapper = AdresseMapper(listOf(pdflAdresse))

        adresseMapper.postAdresse?.adresselinje1 shouldBe "postbokseier"
        adresseMapper.postAdresse?.adresselinje2 shouldBe "postboks"
        adresseMapper.postAdresse?.adresselinje3 shouldBe ""
        adresseMapper.postAdresse?.postnummer shouldBe "1453"
        adresseMapper.postAdresse?.poststed shouldBe "Bjørnemyr"
        adresseMapper.postAdresse?.landkode shouldBe "NO"
        adresseMapper.postAdresse?.land shouldBe "NORGE"
    }

    @Test
    fun `AdresseMapper mapper PDLAdresse_PostboksAdresse til forventede verdier hvis PDL returnerer adresse uten verdier`() {
        val pdflAdresse =
            PDLAdresse.PostboksAdresse(
                adresseMetadata =
                    AdresseMetadata(
                        adresseType = BOSTEDSADRESSE,
                        master = MasterType.PDL,
                    ),
                postbokseier = null,
                postboks = null,
                postnummer = null,
            )

        val adresseMapper = AdresseMapper(listOf(pdflAdresse))

        adresseMapper.postAdresse?.adresselinje1 shouldBe ""
        adresseMapper.postAdresse?.adresselinje2 shouldBe ""
        adresseMapper.postAdresse?.adresselinje3 shouldBe ""
        adresseMapper.postAdresse?.postnummer shouldBe ""
        adresseMapper.postAdresse?.poststed shouldBe ""
        adresseMapper.postAdresse?.landkode shouldBe "NO"
        adresseMapper.postAdresse?.land shouldBe "NORGE"
    }

    @Test
    fun `AdresseMapper mapper PDLAdresse_UtenlandsAdresseIFrittFormat til forventede verdier hvis PDL returnerer adresse med verdier`() {
        val pdflAdresse =
            PDLAdresse.UtenlandsAdresseIFrittFormat(
                adresseMetadata =
                    AdresseMetadata(
                        adresseType = BOSTEDSADRESSE,
                        master = MasterType.PDL,
                    ),
                adresseLinje1 = "adr1",
                adresseLinje2 = "adr2",
                adresseLinje3 = "adr3",
                postkode = "N12-W4",
                byEllerStedsnavn = "byEllerStedsnavn",
                landKode = "SE",
            )

        val adresseMapper = AdresseMapper(listOf(pdflAdresse))

        adresseMapper.postAdresse?.adresselinje1 shouldBe "adr1"
        adresseMapper.postAdresse?.adresselinje2 shouldBe "adr2"
        adresseMapper.postAdresse?.adresselinje3 shouldBe "adr3"
        adresseMapper.postAdresse?.postnummer shouldBe "N12-W4"
        adresseMapper.postAdresse?.poststed shouldBe "byEllerStedsnavn"
        adresseMapper.postAdresse?.landkode shouldBe "SE"
        adresseMapper.postAdresse?.land shouldBe "SVERIGE"
    }

    @Test
    fun `AdresseMapper mapper PDLAdresse_UtenlandsAdresseIFrittFormat til forventede verdier hvis PDL returnerer adresse uten verdier`() {
        val pdflAdresse =
            PDLAdresse.UtenlandsAdresseIFrittFormat(
                adresseMetadata =
                    AdresseMetadata(
                        adresseType = BOSTEDSADRESSE,
                        master = MasterType.PDL,
                    ),
                adresseLinje1 = null,
                adresseLinje2 = null,
                adresseLinje3 = null,
                postkode = null,
                byEllerStedsnavn = null,
                landKode = null,
            )

        val adresseMapper = AdresseMapper(listOf(pdflAdresse))

        adresseMapper.postAdresse?.adresselinje1 shouldBe ""
        adresseMapper.postAdresse?.adresselinje2 shouldBe ""
        adresseMapper.postAdresse?.adresselinje3 shouldBe ""
        adresseMapper.postAdresse?.postnummer shouldBe ""
        adresseMapper.postAdresse?.poststed shouldBe ""
        adresseMapper.postAdresse?.landkode shouldBe ""
        adresseMapper.postAdresse?.land shouldBe ""
    }

    @Test
    fun `AdresseMapper mapper PDLAdresse_UtenlandskAdresse til forventede verdier hvis PDL returnerer adresse med verdier`() {
        val pdflAdresse =
            PDLAdresse.UtenlandskAdresse(
                adresseMetadata =
                    AdresseMetadata(
                        adresseType = BOSTEDSADRESSE,
                        master = MasterType.PDL,
                    ),
                adressenavnNummer = "adressenavnNummer",
                bygningEtasjeLeilighet = "bygningEtasjeLeilighet",
                postboksNummerNavn = "postboksNummerNavn",
                postkode = "N12-W4",
                bySted = "bySted",
                landKode = "DK",
            )

        val adresseMapper = AdresseMapper(listOf(pdflAdresse))

        adresseMapper.postAdresse?.adresselinje1 shouldBe "adressenavnNummer"
        adresseMapper.postAdresse?.adresselinje2 shouldBe "bygningEtasjeLeilighet"
        adresseMapper.postAdresse?.adresselinje3 shouldBe "postboksNummerNavn"
        adresseMapper.postAdresse?.postnummer shouldBe "N12-W4"
        adresseMapper.postAdresse?.poststed shouldBe "bySted"
        adresseMapper.postAdresse?.landkode shouldBe "DK"
        adresseMapper.postAdresse?.land shouldBe "DANMARK"
    }

    @Test
    fun `AdresseMapper mapper PDLAdresse_UtenlandskAdresse til forventede verdier hvis PDL returnerer adresse uten verdier`() {
        val pdflAdresse =
            PDLAdresse.UtenlandskAdresse(
                adresseMetadata =
                    AdresseMetadata(
                        adresseType = BOSTEDSADRESSE,
                        master = MasterType.PDL,
                    ),
                adressenavnNummer = null,
                bygningEtasjeLeilighet = null,
                postboksNummerNavn = null,
                postkode = null,
                bySted = null,
                landKode = null,
            )

        val adresseMapper = AdresseMapper(listOf(pdflAdresse))

        adresseMapper.postAdresse?.adresselinje1 shouldBe ""
        adresseMapper.postAdresse?.adresselinje2 shouldBe ""
        adresseMapper.postAdresse?.adresselinje3 shouldBe ""
        adresseMapper.postAdresse?.postnummer shouldBe ""
        adresseMapper.postAdresse?.poststed shouldBe ""
        adresseMapper.postAdresse?.landkode shouldBe ""
        adresseMapper.postAdresse?.land shouldBe ""
    }

    @Test
    fun `AdresseMapper mapper PDLAdresse_VegAdresse til forventede verdier hvis PDL returnerer adresse med verdier`() {
        val pdflAdresse =
            PDLAdresse.VegAdresse(
                adresseMetadata =
                    AdresseMetadata(
                        coAdresseNavn = "coAdresseNavn",
                        adresseType = BOSTEDSADRESSE,
                        master = MasterType.PDL,
                    ),
                husnummer = "husnummer",
                husbokstav = "husbokstav",
                adressenavn = "adressenavn",
                postnummer = "1453",
            )

        val adresseMapper = AdresseMapper(listOf(pdflAdresse))

        adresseMapper.postAdresse?.adresselinje1 shouldBe "coAdresseNavn"
        adresseMapper.postAdresse?.adresselinje2 shouldBe "adressenavn husnummerhusbokstav"
        adresseMapper.postAdresse?.postnummer shouldBe "1453"
        adresseMapper.postAdresse?.poststed shouldBe "Bjørnemyr"
        adresseMapper.postAdresse?.landkode shouldBe "NO"
        adresseMapper.postAdresse?.land shouldBe "NORGE"
    }

    @Test
    fun `AdresseMapper mapper PDLAdresse_VegAdresse til forventede verdier hvis PDL returnerer adresse uten verdier`() {
        val pdflAdresse =
            PDLAdresse.VegAdresse(
                adresseMetadata =
                    AdresseMetadata(
                        coAdresseNavn = null,
                        adresseType = BOSTEDSADRESSE,
                        master = MasterType.PDL,
                    ),
                husnummer = null,
                husbokstav = null,
                adressenavn = null,
                postnummer = null,
            )

        val adresseMapper = AdresseMapper(listOf(pdflAdresse))

        adresseMapper.postAdresse?.adresselinje1 shouldBe ""
        adresseMapper.postAdresse?.adresselinje2 shouldBe ""
        adresseMapper.postAdresse?.postnummer shouldBe ""
        adresseMapper.postAdresse?.poststed shouldBe ""
        adresseMapper.postAdresse?.landkode shouldBe "NO"
        adresseMapper.postAdresse?.land shouldBe "NORGE"
    }

    @Test
    fun `AdresseMapper mapper PDLAdresse_TomAdresse til forventede verdier`() {
        AdresseMapper(listOf(PDLAdresse.TomAdresse)).postAdresse shouldBe AdresseDto()
    }

    @Test
    fun `AdresseMapper mapper PDLAdresse_MatrikkelAdresse til forventede verdier`() {
        val pdlAdresse =
            PDLAdresse.MatrikkelAdresse(
                adresseMetadata =
                    AdresseMetadata(adresseType = BOSTEDSADRESSE, master = MasterType.PDL),
            )

        AdresseMapper(listOf(pdlAdresse)).postAdresse shouldBe AdresseDto()
    }

    private fun createPdlAdresse(
        adresseType: AdresseType,
        postnummer: String,
        gyldigFom: LocalDate? = LocalDate.now(),
    ): PDLAdresse =
        PDLAdresse.PostboksAdresse(
            adresseMetadata =
                AdresseMetadata(
                    gyldigFom = gyldigFom,
                    adresseType = adresseType,
                    master = MasterType.PDL,
                ),
            postnummer = postnummer,
        )
}
