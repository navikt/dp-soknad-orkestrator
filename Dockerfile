FROM gcr.io/distroless/java21

COPY build/libs/*-all.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]