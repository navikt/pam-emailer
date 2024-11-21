FROM ghcr.io/navikt/baseimages/temurin:21
COPY build/libs/pam-emailer-0.1.jar ./app.jar
ENV JAVA_OPTS="-Xms256m -Xmx1024m"

