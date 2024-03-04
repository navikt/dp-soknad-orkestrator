package no.nav.dagpenger.soknad.orkestrator.søknad

import com.zaxxer.hikari.HikariDataSource
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.util.UUID
import javax.sql.DataSource

class OpplysningRepositoryPostgresTest {
    private val database = PostgreSQLContainer("postgres:15")
    private val dataSource: DataSource

    private var opplysningRepository: OpplysningRepository

    init {
        database.start()
        dataSource =
            HikariDataSource().apply {
                jdbcUrl = database.jdbcUrl
                username = database.username
                password = database.password
            }
        opplysningRepository = OpplysningRepositoryPostgres(dataSource)
    }

    @Test
    fun `vi kan lagre opplysning`() {
        val beskrivendeId = "beskrivendeId"
        val fødselsnummer = "12345678901"
        val søknadsId = UUID.randomUUID()
        val opplysning =
            Opplysning(
                beskrivendeId = beskrivendeId,
                svar = listOf("svar1"),
                fødselsnummer = fødselsnummer,
                søknadsId = søknadsId,
            )

        opplysningRepository.lagre(opplysning)

        val hentetOpplysning =
            opplysningRepository.hent(
                beskrivendeId,
                fødselsnummer,
                søknadsId,
            )

        hentetOpplysning.beskrivendeId() shouldBe beskrivendeId
        hentetOpplysning.svar() shouldBe listOf("svar1")
        hentetOpplysning.fødselsnummer shouldBe fødselsnummer
    }
}
