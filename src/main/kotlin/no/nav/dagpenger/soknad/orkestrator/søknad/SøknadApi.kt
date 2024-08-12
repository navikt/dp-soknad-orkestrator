package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import no.nav.dagpenger.soknad.orkestrator.api.auth.ident
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalTypeDTO.BOOLEAN
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalTypeDTO.DATO
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalTypeDTO.LAND
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalTypeDTO.PERIODE
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalTypeDTO.TEKST
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.PeriodeSvar
import no.nav.dagpenger.soknad.orkestrator.spørsmål.BooleanSvar
import no.nav.dagpenger.soknad.orkestrator.spørsmål.DatoSvar
import no.nav.dagpenger.soknad.orkestrator.spørsmål.LandSvar
import no.nav.dagpenger.soknad.orkestrator.spørsmål.PeriodesvarSvar
import no.nav.dagpenger.soknad.orkestrator.spørsmål.TekstSvar
import java.time.LocalDate
import java.util.UUID

internal fun Application.søknadApi(søknadService: SøknadService) {
    routing {
        authenticate("tokenX") {
            route("/soknad") {
                post("/start") {
                    val søknad = søknadService.opprettSøknad(call.ident())

                    call.respond(HttpStatusCode.Created, søknad.søknadId)
                }

                get("/{søknadId}/neste") {
                    val søknadId = søknadId()

                    val nesteSpørsmålgruppe = søknadService.nesteSpørsmålgruppe(søknadId)

                    call.respond(HttpStatusCode.OK, nesteSpørsmålgruppe)
                }

                post("/{søknadId}/svar") {
                    val søknadId = søknadId()
                    val besvartSpørsmål = call.receive<SporsmalDTO>()

                    besvartSpørsmål.validerSvar()

                    val svar =
                        when (besvartSpørsmål.type) {
                            LAND -> LandSvar(verdi = besvartSpørsmål.svar!!)

                            DATO -> DatoSvar(verdi = LocalDate.parse(besvartSpørsmål.svar))

                            TEKST -> TekstSvar(verdi = besvartSpørsmål.svar!!)

                            BOOLEAN -> BooleanSvar(verdi = besvartSpørsmål.svar.toBoolean())

                            PERIODE ->
                                PeriodesvarSvar(verdi = objectMapper.readValue<PeriodeSvar>(besvartSpørsmål.svar!!))
                        }

                    søknadService.håndterSvar(søknadId = søknadId, spørsmålId = besvartSpørsmål.id, svar = svar)

                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

internal fun PipelineContext<Unit, ApplicationCall>.søknadId() =
    call.parameters["søknadId"].let { UUID.fromString(it) }
        ?: throw IllegalArgumentException("Må ha med id i parameter")

fun SporsmalDTO.validerSvar() {
    if (svar == null) {
        throw IllegalArgumentException("Svar kan ikke være null")
    }

    when (type) {
        LAND -> {
            require(svar!!.length == 3) { "Landkode må være tre bokstaver" }
        }

        PERIODE -> {
            try {
                objectMapper.readValue<PeriodeSvar>(svar!!)
            } catch (e: Exception) {
                throw IllegalArgumentException("Kunne ikke parse svar til periode")
            }
        }

        DATO -> {
            try {
                LocalDate.parse(svar)
            } catch (e: Exception) {
                throw IllegalArgumentException("Kunne ikke parse svar til dato")
            }
        }

        BOOLEAN -> {
            require(svar == "true" || svar == "false") { "Svar må være true eller false" }
        }

        TEKST -> { // Trenger ikke validering
        }
    }
}
