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

    val soknadVarslet: Counter =
        Counter.build()
            .namespace(NAMESPACE)
            .name("antall_soknader_varslet")
            .help("Indikerer antall sendte varsler om søknader")
            .register()

    val behovMottatt: Counter =
        Counter.build()
            .namespace(NAMESPACE)
            .name("antall_behov_mottatt")
            .help("Indikerer antall og type mottatte behov")
            .labelNames("behov")
            .register()

    val behovLost: Counter =
        Counter.build()
            .namespace(NAMESPACE)
            .name("antall_behov_lost")
            .help("Indikerer antall og type behov som er løst")
            .labelNames("behov")
            .register()
}
