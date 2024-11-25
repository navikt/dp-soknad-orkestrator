FROM gcr.io/distroless/java21

COPY build/libs/*-all.jar app.jar

CMD ["app.jar"]