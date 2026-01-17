# syntax=docker/dockerfile:1

# Comments are provided throughout this file to help you get started.
# If you need more help, visit the Dockerfile reference guide at
# https://docs.docker.com/go/dockerfile-reference/

# Want to help us make this template better? Share your feedback here: https://forms.gle/ybq9Krt8jtBL3iCk7

################################################################################

# Create a stage for resolving and downloading dependencies.
FROM eclipse-temurin:21-jdk as deps
WORKDIR /build

# 1. Préparation des Commons (Installation dans le repo local)
COPY dvdtheque-commons/ dvdtheque-commons/
COPY dvdtheque-allocine-service/mvnw dvdtheque-allocine-service/mvnw
COPY dvdtheque-allocine-service/.mvn/ dvdtheque-allocine-service/.mvn/

RUN chmod +x dvdtheque-allocine-service/mvnw && \
    cd dvdtheque-commons && \
    ../dvdtheque-allocine-service/mvnw install -DskipTests

# 2. Préparation du Service (Download des dépendances)
COPY dvdtheque-allocine-service/pom.xml dvdtheque-allocine-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    cd dvdtheque-allocine-service && ./mvnw dependency:go-offline -DskipTests
################################################################################

# Create a stage for building the application based on the stage with downloaded dependencies.
# This Dockerfile is optimized for Java applications that output an uber jar, which includes
# all the dependencies needed to run your app inside a JVM. If your app doesn't output an uber
# jar and instead relies on an application server like Apache Tomcat, you'll need to update this
# stage with the correct filename of your package and update the base image of the "final" stage
# use the relevant app server, e.g., using tomcat (https://hub.docker.com/_/tomcat/) as a base image.
FROM deps as package
WORKDIR /build

# On copie les sources du service
COPY dvdtheque-allocine-service/src dvdtheque-allocine-service/src

# COMPILATION ET RENOMMAGE SÉCURISÉ
RUN --mount=type=cache,target=/root/.m2 \
    cd dvdtheque-allocine-service && \
    ./mvnw package -DskipTests && \
    # On cherche le JAR le plus lourd (le Fat JAR de Spring Boot)
    # ou on exclut explicitement le JAR "original"
    cp target/$(ls target/ | grep .jar | grep -v original | head -n 1) target/app.jar

################################################################################

# Create a stage for extracting the application into separate layers.
# Take advantage of Spring Boot's layer tools and Docker's caching by extracting
# the packaged application into separate layers that can be copied into the final stage.
# See Spring's docs for reference:
# https://docs.spring.io/spring-boot/docs/current/reference/html/container-images.html
FROM package as extract
WORKDIR /build

# On pointe précisément sur le jar dans le sous-dossier
RUN java -Djarmode=layertools -jar dvdtheque-allocine-service/target/app.jar extract --destination target/extracted

################################################################################

# Create a new stage for running the application that contains the minimal
# runtime dependencies for the application. This often uses a different base
# image from the install or build stage where the necessary files are copied
# from the install stage.
#
# The example below uses eclipse-turmin's JRE image as the foundation for running the app.
# By specifying the "17-jre-jammy" tag, it will also use whatever happens to be the
# most recent version of that tag when you build your Dockerfile.
# If reproducibility is important, consider using a specific digest SHA, like
# eclipse-temurin@sha256:99cede493dfd88720b610eb8077c8688d3cca50003d76d1d539b0efc8cca72b4.
FROM eclipse-temurin:21-jre AS final

# Create a non-privileged user that the app will run under.
# See https://docs.docker.com/go/dockerfile-user-best-practices/
ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser
USER appuser

# Copy the executable from the "package" stage.
COPY --from=extract build/target/extracted/dependencies/ ./
COPY --from=extract build/target/extracted/spring-boot-loader/ ./
COPY --from=extract build/target/extracted/snapshot-dependencies/ ./
COPY --from=extract build/target/extracted/application/ ./

EXPOSE 8087

ENTRYPOINT [ "java", "-Dspring.profiles.active=docker","-Dspring.cloud.config.label=develop", "org.springframework.boot.loader.launch.JarLauncher" ]
