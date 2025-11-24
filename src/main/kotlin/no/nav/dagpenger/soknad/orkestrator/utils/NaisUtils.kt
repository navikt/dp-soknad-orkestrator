package no.nav.dagpenger.soknad.orkestrator.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking

class NaisUtils {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun isLeader(httpClient: HttpClient): Boolean {
        return try {
            val electorGetUrl = getSystemEnv("ELECTOR_GET_URL")
            val hostname = getSystemEnv("HOSTNAME")

            if (electorGetUrl.isNullOrBlank()) {
                logger.error {
                    "Environment property \"ELECTOR_GET_URL\" er null eller blank. Er leader election skrudd på? " +
                        "Se https://docs.nais.io/services/leader-election/how-to/enable/"
                }
                return false
            }

            if (hostname.isNullOrBlank()) {
                logger.error { "Environment property \"HOSTNAME\" er null eller blank" }
                return false
            }

            val leader =
                runBlocking {
                    httpClient.get(electorGetUrl).body<Leader>().name
                }

            logger.debug { "Denne podden er \"$hostname\", leader pod er \"$leader\"" }

            hostname == leader
        } catch (e: Exception) {
            logger.error(e) { "Sjekk av om pod er leader feilet" }
            false
        }
    }

    private data class Leader(
        val name: String,
    )
}

fun getSystemEnv(key: String): String? = System.getenv(key)
