FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21

COPY build/libs/pam-emailer-0.1.jar /app.jar

ENV JAVA_OPTS="-Xms256m -Xmx1024m"

ENTRYPOINT ["java", "-jar", "/app.jar"]