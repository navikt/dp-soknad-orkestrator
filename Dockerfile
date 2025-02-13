FROM gcr.io/distroless/java21

COPY build/libs/*-all.jar app.jar

CMD ["java", "-jar", "app.jar"]