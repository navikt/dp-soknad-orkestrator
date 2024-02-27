package no.nav.dagpenger.soknad.orkestrator

import no.nav.dagpenger.soknad.orkestrator.config.Configuration

fun main() {
    ApplicationBuilder(Configuration.config).start()
}
