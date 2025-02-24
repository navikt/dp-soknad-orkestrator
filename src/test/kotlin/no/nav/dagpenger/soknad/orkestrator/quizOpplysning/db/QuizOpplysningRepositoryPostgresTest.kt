package no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db

import QuizOpplysningTabell
import TekstTabell
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Barn
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Boolsk
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Dato
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Desimaltall
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.EgenNæring
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.EøsArbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.EøsArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Flervalg
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Heltall
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Periode
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.PeriodeSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.utils.januar
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class QuizOpplysningRepositoryPostgresTest {
    private var opplysningRepository = QuizOpplysningRepositoryPostgres(dataSource)
    private val beskrivendeId = "beskrivendeId"
    private val ident = "12345678901"
    private val søknadId = UUID.randomUUID()

    @Test
    fun `vi kan lagre og hente opplysning av type tekst`() {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = beskrivendeId,
                type = Tekst,
                svar = "svar",
                ident = ident,
                søknadId = søknadId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi kan lagre og hente opplysning av type heltall`() {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = beskrivendeId,
                type = Heltall,
                svar = 10,
                ident = ident,
                søknadId = søknadId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi kan lagre og hente opplysning av type desimaltall`() {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = beskrivendeId,
                type = Desimaltall,
                svar = 10.5,
                ident = ident,
                søknadId = søknadId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi kan lagre og hente opplysning av type boolsk`() {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = beskrivendeId,
                type = Boolsk,
                svar = true,
                ident = ident,
                søknadId = søknadId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi kan lagre og hente opplysning av type dato`() {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = beskrivendeId,
                type = Dato,
                svar = LocalDate.now(),
                ident = ident,
                søknadId = søknadId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi kan lagre og hente opplysning av type flervalg`() {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = beskrivendeId,
                type = Flervalg,
                svar = listOf("svar1", "svar2", "svar3"),
                ident = ident,
                søknadId = søknadId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi kan lagre og hente opplysning av type periode`() {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = beskrivendeId,
                type = Periode,
                svar = PeriodeSvar(LocalDate.now(), LocalDate.now().plusDays(10)),
                ident = ident,
                søknadId = søknadId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi kan lagre og hente opplysning av type generator - arbeidsforhold`() {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = beskrivendeId,
                type = Arbeidsforhold,
                svar =
                    listOf(
                        ArbeidsforholdSvar(navn = "navn", land = "land", sluttårsak = Sluttårsak.PERMITTERT),
                        ArbeidsforholdSvar(navn = "navn2", land = "land2", sluttårsak = Sluttårsak.AVSKJEDIGET),
                    ),
                ident = ident,
                søknadId = søknadId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi kan lagre og hente opplysning av type generator - eøs arbeidsforhold`() {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = beskrivendeId,
                type = EøsArbeidsforhold,
                svar =
                    listOf(
                        EøsArbeidsforholdSvar(
                            bedriftsnavn = "arbeidsgivernavn",
                            land = "land",
                            personnummerIArbeidsland = "personnummer",
                            varighet = PeriodeSvar(LocalDate.now(), LocalDate.now().plusDays(10)),
                        ),
                        EøsArbeidsforholdSvar(
                            bedriftsnavn = "arbeidsgivernavn2",
                            land = "land2",
                            personnummerIArbeidsland = "personnummer2",
                            varighet = PeriodeSvar(LocalDate.now(), LocalDate.now().plusDays(10)),
                        ),
                    ),
                ident = ident,
                søknadId = søknadId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi kan lagre og hente opplysning av type generator - egen næring`() {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = beskrivendeId,
                type = EgenNæring,
                svar = listOf(123456789, 987654321),
                ident = ident,
                søknadId = søknadId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi kan lagre og hente opplysning av type generator - barn`() {
        val barnSvarId1 = UUID.randomUUID()
        val barnSvarId2 = UUID.randomUUID()
        val opplysning =
            QuizOpplysning(
                beskrivendeId = beskrivendeId,
                type = Barn,
                svar =
                    listOf(
                        BarnSvar(
                            barnSvarId = barnSvarId1,
                            fornavnOgMellomnavn = "Fornavn Mellomnavn",
                            etternavn = "Etternavn",
                            fødselsdato = 1.januar(2024),
                            statsborgerskap = "NOR",
                            forsørgerBarnet = true,
                            fraRegister = false,
                            kvalifisererTilBarnetillegg = true,
                        ),
                        BarnSvar(
                            barnSvarId = barnSvarId2,
                            fornavnOgMellomnavn = "Fornavn Mellomnavn Register",
                            etternavn = "Etternavn Register",
                            fødselsdato = 1.januar(2024),
                            statsborgerskap = "NOR",
                            forsørgerBarnet = true,
                            fraRegister = true,
                            kvalifisererTilBarnetillegg = true,
                        ),
                    ),
                ident = ident,
                søknadId = søknadId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(beskrivendeId, ident, søknadId)!!.also {
                it.beskrivendeId shouldBe opplysning.beskrivendeId
                it.type shouldBe opplysning.type
                it.svar.asListOf<BarnSvar>().first().barnSvarId shouldBe barnSvarId1
                it.svar.asListOf<BarnSvar>().last().barnSvarId shouldBe barnSvarId2
                it.ident shouldBe opplysning.ident
                it.søknadId shouldBe opplysning.søknadId
            }
        }
    }

    @Test
    fun `vi lagrer ikke opplysning dersom den allerede er lagret`() {
        val opplysning1 = opplysning(søknadId = søknadId)
        val opplysning2 = opplysning(søknadId = søknadId)

        withMigratedDb {
            opplysningRepository.lagre(opplysning1)
            val antallEtterFørsteLagring = transaction { QuizOpplysningTabell.selectAll().count() }
            antallEtterFørsteLagring shouldBe 1

            opplysningRepository.lagre(opplysning2)
            val antallEtterAndreLagring = transaction { QuizOpplysningTabell.selectAll().count() }
            antallEtterAndreLagring shouldBe 1
        }
    }

    @Test
    fun `vi returnerer null dersom det ikke finnes en opplysning med gitte kriterier`() {
        withMigratedDb {
            opplysningRepository.hent(
                beskrivendeId = "random",
                ident = "123",
                søknadId = UUID.randomUUID(),
            ) shouldBe null
        }
    }

    @Test
    fun `vi kan slette en opplysning og tilhørende svar`() {
        val opplysning = opplysning(søknadId = søknadId)

        withMigratedDb {
            opplysningRepository.lagre(opplysning)
            val antallOpplysningerEtterLagring = transaction { QuizOpplysningTabell.selectAll().count() }
            val antallTekstsvarEtterLagring = transaction { TekstTabell.selectAll().count() }

            antallOpplysningerEtterLagring shouldBe 1
            antallTekstsvarEtterLagring shouldBe 1
            opplysningRepository.slett(søknadId)
            val antallOpplysningerEtterSletting = transaction { QuizOpplysningTabell.selectAll().count() }
            val antallTekstsvarEtterSletting = transaction { TekstTabell.selectAll().count() }
            antallOpplysningerEtterSletting shouldBe 0
            antallTekstsvarEtterSletting shouldBe 0
        }
    }
}

fun opplysning(
    beskrivendeId: String = "beskrivendeId",
    ident: String = "12345678901",
    søknadId: UUID = UUID.randomUUID(),
) = QuizOpplysning(
    beskrivendeId = beskrivendeId,
    type = Tekst,
    svar = "svar",
    ident = ident,
    søknadId = søknadId,
)
