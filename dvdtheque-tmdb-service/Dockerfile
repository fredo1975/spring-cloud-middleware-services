# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk as deps
WORKDIR /build

# 1. Préparation des Commons (Installation dans le repo local)
COPY dvdtheque-commons/ dvdtheque-commons/
COPY dvdtheque-tmdb-service/mvnw dvdtheque-tmdb-service/mvnw
COPY dvdtheque-tmdb-service/.mvn/ dvdtheque-tmdb-service/.mvn/

RUN chmod +x dvdtheque-tmdb-service/mvnw && \
    cd dvdtheque-commons && \
    ../dvdtheque-tmdb-service/mvnw install -DskipTests

# 2. Préparation du Service (Download des dépendances)
COPY dvdtheque-tmdb-service/pom.xml dvdtheque-tmdb-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    cd dvdtheque-tmdb-service && ./mvnw dependency:go-offline -DskipTests
################################################################################

FROM deps as package
WORKDIR /build

# On copie les sources du service
COPY dvdtheque-tmdb-service/src dvdtheque-tmdb-service/src

# COMPILATION ET RENOMMAGE SÉCURISÉ
RUN --mount=type=cache,target=/root/.m2 \
    cd dvdtheque-tmdb-service && \
    ./mvnw package -DskipTests && \
    # Utilisation de find pour trouver le bon JAR (l'exécutable) et le renommer proprement
    find target/ -name "*.jar" -not -name "*original*" -exec cp {} target/app.jar \;

################################################################################

# Create a stage for extracting the application into separate layers.
# Take advantage of Spring Boot's layer tools and Docker's caching by extracting
# the packaged application into separate layers that can be copied into the final stage.
# See Spring's docs for reference:
# https://docs.spring.io/spring-boot/docs/current/reference/html/container-images.html
FROM package as extract
WORKDIR /build

# On pointe précisément sur le jar dans le sous-dossier
RUN java -Djarmode=layertools -jar dvdtheque-tmdb-service/target/app.jar extract --destination target/extracted

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

EXPOSE 8086

ENTRYPOINT [ "java", "-Dspring.profiles.active=docker","-Dspring.cloud.config.label=develop", "org.springframework.boot.loader.launch.JarLauncher" ]
