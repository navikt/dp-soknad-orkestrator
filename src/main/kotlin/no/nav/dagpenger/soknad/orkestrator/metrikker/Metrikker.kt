package no.nav.dagpenger.soknad.orkestrator.metrikker

import io.prometheus.metrics.core.metrics.Counter

private const val NAMESPACE = "dp_soknad_orkestrator"

object SøknadMetrikker {
    val opprettet: Counter =
        Counter
            .builder()
            .name("${NAMESPACE}_antall_soknader_opprettet")
            .help("Indikerer antall nye søknader som er opprettet (ny søknad-flyt)")
            .register()

    val mottatt: Counter =
        Counter
            .builder()
            .name("${NAMESPACE}_antall_soknader_mottatt")
            .help("Indikerer antall søknader som er mottatt/innsendt. Label 'kilde': 'ny' (orkestrator) eller 'quiz' (gammel flyt)")
            .labelNames("kilde")
            .register()

    val slettet: Counter =
        Counter
            .builder()
            .name("${NAMESPACE}_antall_soknader_slettet")
            .help("Indikerer antall søknader som er slettet. Label 'kilde': 'ny' (orkestrator) eller 'quiz' (gammel flyt)")
            .labelNames("kilde")
            .register()

    val journalfort: Counter =
        Counter
            .builder()
            .name("${NAMESPACE}_antall_soknader_journalfort")
            .help("Indikerer antall søknader som er journalført (ny søknad-flyt)")
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

object OpplysningMetrikker {
    val endringBarn: Counter =
        Counter
            .builder()
            .name("${NAMESPACE}_antall_endringer_barn")
            .help("Indikerer antall ganger opplysning om barn er endret")
            .register()
}
