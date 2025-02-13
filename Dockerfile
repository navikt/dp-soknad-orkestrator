FROM gcr.io/distroless/java21

ENV TZ="Europe/Oslo"

COPY build/libs/*-all.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]