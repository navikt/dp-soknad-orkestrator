FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:bec6cea1c5412108a06228ef05e212f91f1f98425ee745cddcdc4aeb067e3723

ENV TZ="Europe/Oslo"

COPY build/install/dp-soknad-orkestrator/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.soknad.orkestrator.ApplicationKt"]