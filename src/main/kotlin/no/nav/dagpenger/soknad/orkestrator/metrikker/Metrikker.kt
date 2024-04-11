package no.nav.dagpenger.soknad.orkestrator.metrikker

import io.prometheus.client.Counter

private const val NAMESPACE = "dp_soknad_orkestrator"

object Metrikker {
    val sokaderMottatt: Counter =
        Counter.build()
            .namespace(NAMESPACE)
            .name("antall_soknader_mottatt")
            .help("Indikerer antall søknader som er mottatt")
            .register()

    val varselSoknad: Counter =
        Counter.build()
            .namespace(NAMESPACE)
            .name("antall_soknader_varsel")
            .help("Indikerer antall sendte varsler om søknader")
            .register()
}
