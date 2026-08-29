FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:f7eb785ce65c71eb7219a6e02422f05e794274edae5d31b6050e05c021550901

ENV TZ="Europe/Oslo"

COPY build/install/dp-soknad-orkestrator/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.soknad.orkestrator.ApplicationKt"]