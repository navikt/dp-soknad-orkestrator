package no.nav.dagpenger.soknad.orkestrator.opplysning.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.opplysning.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.Boolsk
import no.nav.dagpenger.soknad.orkestrator.opplysning.Dato
import no.nav.dagpenger.soknad.orkestrator.opplysning.Desimaltall
import no.nav.dagpenger.soknad.orkestrator.opplysning.EøsArbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.EøsArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.Flervalg
import no.nav.dagpenger.soknad.orkestrator.opplysning.Heltall
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.Periode
import no.nav.dagpenger.soknad.orkestrator.opplysning.PeriodeSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.Tekst
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.NoSuchElementException
import java.util.UUID

class OpplysningRepositoryPostgresTest {
    private var opplysningRepository = OpplysningRepositoryPostgres(dataSource)
    private val beskrivendeId = "beskrivendeId"
    private val ident = "12345678901"
    private val søknadsId = UUID.randomUUID()

    @Test
    fun `vi kan lagre og hente opplysning av type tekst`() {
        val opplysning =
            Opplysning(
                beskrivendeId = beskrivendeId,
                type = Tekst,
                svar = "svar",
                ident = ident,
                søknadsId = søknadsId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadsId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi kan lagre og hente opplysning av type heltall`() {
        val opplysning =
            Opplysning(
                beskrivendeId = beskrivendeId,
                type = Heltall,
                svar = 10,
                ident = ident,
                søknadsId = søknadsId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadsId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi kan lagre og hente opplysning av type desimaltall`() {
        val opplysning =
            Opplysning(
                beskrivendeId = beskrivendeId,
                type = Desimaltall,
                svar = 10.5,
                ident = ident,
                søknadsId = søknadsId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadsId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi kan lagre og hente opplysning av type boolsk`() {
        val opplysning =
            Opplysning(
                beskrivendeId = beskrivendeId,
                type = Boolsk,
                svar = true,
                ident = ident,
                søknadsId = søknadsId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadsId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi kan lagre og hente opplysning av type dato`() {
        val opplysning =
            Opplysning(
                beskrivendeId = beskrivendeId,
                type = Dato,
                svar = LocalDate.now(),
                ident = ident,
                søknadsId = søknadsId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadsId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi kan lagre og hente opplysning av type flervalg`() {
        val opplysning =
            Opplysning(
                beskrivendeId = beskrivendeId,
                type = Flervalg,
                svar = listOf("svar1", "svar2", "svar3"),
                ident = ident,
                søknadsId = søknadsId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadsId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi kan lagre og hente opplysning av type periode`() {
        val opplysning =
            Opplysning(
                beskrivendeId = beskrivendeId,
                type = Periode,
                svar = PeriodeSvar(LocalDate.now(), LocalDate.now().plusDays(10)),
                ident = ident,
                søknadsId = søknadsId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadsId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi kan lagre og hente opplysning av type generator - arbeidsforhold`() {
        val opplysning =
            Opplysning(
                beskrivendeId = beskrivendeId,
                type = Arbeidsforhold,
                svar =
                    listOf(
                        ArbeidsforholdSvar(navn = "navn", land = "land"),
                        ArbeidsforholdSvar(navn = "navn2", land = "land2"),
                    ),
                ident = ident,
                søknadsId = søknadsId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadsId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi kan lagre og hente opplysning av type generator - eøs arbeidsforhold`() {
        val opplysning =
            Opplysning(
                beskrivendeId = beskrivendeId,
                type = EøsArbeidsforhold,
                svar =
                    listOf(
                        EøsArbeidsforholdSvar(
                            bedriftnavn = "arbeidsgivernavn",
                            land = "land",
                            personnummerIArbeidsland = "personnummer",
                            varighet = PeriodeSvar(LocalDate.now(), LocalDate.now().plusDays(10)),
                        ),
                        EøsArbeidsforholdSvar(
                            bedriftnavn = "arbeidsgivernavn2",
                            land = "land2",
                            personnummerIArbeidsland = "personnummer2",
                            varighet = PeriodeSvar(LocalDate.now(), LocalDate.now().plusDays(10)),
                        ),
                    ),
                ident = ident,
                søknadsId = søknadsId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadsId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi lagrer ikke opplysning dersom den allerede er lagret`() {
        val opplysning1 = opplysning(søknadsId = søknadsId)
        val opplysning2 = opplysning(søknadsId = søknadsId)

        withMigratedDb {
            opplysningRepository.lagre(opplysning1)
            val antallEtterFørsteLagring = transaction { OpplysningTabell.selectAll().count() }
            antallEtterFørsteLagring shouldBe 1

            opplysningRepository.lagre(opplysning2)
            val antallEtterAndreLagring = transaction { OpplysningTabell.selectAll().count() }
            antallEtterAndreLagring shouldBe 1
        }
    }

    @Test
    fun `vi henter ikke opplysning dersom ett av kriteriene ikke stemmer`() {
        val opplysning =
            Opplysning(
                beskrivendeId = beskrivendeId,
                type = Tekst,
                svar = "svar",
                ident = ident,
                søknadsId = søknadsId,
            )

        withMigratedDb { opplysningRepository.lagre(opplysning) }

        shouldThrow<NoSuchElementException> {
            opplysningRepository.hent(
                beskrivendeId = beskrivendeId,
                ident = ident,
                søknadsId = UUID.randomUUID(),
            )
        }
    }
}

fun opplysning(
    beskrivendeId: String = "beskrivendeId",
    ident: String = "12345678901",
    søknadsId: UUID = UUID.randomUUID(),
) = Opplysning(
    beskrivendeId = beskrivendeId,
    type = Tekst,
    svar = "svar",
    ident = ident,
    søknadsId = søknadsId,
)
