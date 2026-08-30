FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:930dff6d16150b95fc2e378a7db438839227b041cd25ae05013f9c92ea7a3983

ENV TZ="Europe/Oslo"

COPY build/install/dp-soknad-orkestrator/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.soknad.orkestrator.ApplicationKt"]