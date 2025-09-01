FROM gcr.io/distroless/java21

ENV TZ="Europe/Oslo"

COPY build/install/dp-soknad-orkestrator/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.soknad.orkestrator.ApplicationKt"]