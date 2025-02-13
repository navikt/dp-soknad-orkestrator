FROM ghcr.io/navikt/baseimages/temurin:21
ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

COPY build/libs/*-all.jar app.jar

CMD ["java", "-jar", "app.jar"]