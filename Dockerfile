FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-26@sha256:5a81440744a6c5860256c3ddf85bac3e42c88f291d17d7d0e492112883c0a8bb

ENV TZ="Europe/Oslo"

COPY build/install/dp-soknad-orkestrator/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.soknad.orkestrator.ApplicationKt"]