package no.nav.dagpenger.soknad.orkestrator.metrikker

import io.prometheus.metrics.core.metrics.Counter

private const val NAMESPACE = "dp_soknad_orkestrator"

object SøknadMetrikker {
    val mottatt: Counter =
        Counter
            .builder()
            .name("${NAMESPACE}_antall_soknader_mottatt")
            .help("Indikerer antall søknader som er mottatt")
            .register()

    val slettet: Counter =
        Counter
            .builder()
            .name("${NAMESPACE}_antall_soknader_slettet")
            .help("Indikerer antall søknader som er slettet")
            .register()

    val varslet: Counter =
        Counter
            .builder()
            .name("${NAMESPACE}_antall_soknader_varslet")
            .help("Indikerer antall sendte varsler om søknader")
            .register()

    val dekomponert: Counter =
        Counter
            .builder()
            .name("${NAMESPACE}_antall_soknader_dekomponert")
            .help("Indikerer antall søknader som er dekomponert")
            .register()

    val dekomponeringFeilet: Counter =
        Counter
            .builder()
            .name("${NAMESPACE}_antall_soknader_dekomponering_feilet")
            .help("Indikerer antall søknader som feilet under dekomponering")
            .register()
}

object BehovMetrikker {
    val mottatt: Counter =
        Counter
            .builder()
            .name("${NAMESPACE}_antall_behov_mottatt")
            .help("Indikerer antall mottatte behov")
            .labelNames("behov")
            .register()

    val løst: Counter =
        Counter
            .builder()
            .name("${NAMESPACE}_antall_behov_lost")
            .help("Indikerer antall og type behov som er løst")
            .labelNames("behov")
            .register()
}
