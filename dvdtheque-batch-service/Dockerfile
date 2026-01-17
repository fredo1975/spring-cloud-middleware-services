# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk as deps
WORKDIR /build

# 1. Préparation des Commons (Installation dans le repo local)
COPY dvdtheque-commons/ dvdtheque-commons/
COPY dvdtheque-batch-service/mvnw dvdtheque-batch-service/mvnw
COPY dvdtheque-batch-service/.mvn/ dvdtheque-batch-service/.mvn/

RUN chmod +x dvdtheque-batch-service/mvnw && \
    cd dvdtheque-commons && \
    ../dvdtheque-batch-service/mvnw install -DskipTests

# 2. Préparation du Service (Download des dépendances)
COPY dvdtheque-batch-service/pom.xml dvdtheque-batch-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    cd dvdtheque-batch-service && ./mvnw dependency:go-offline -DskipTests


################################################################################

FROM deps as package
WORKDIR /build

# On copie les sources du service
COPY dvdtheque-batch-service/src dvdtheque-batch-service/src

# COMPILATION ET RENOMMAGE SÉCURISÉ
RUN --mount=type=cache,target=/root/.m2 \
    cd dvdtheque-batch-service && \
    ./mvnw package -DskipTests && \
    # Utilisation de find pour trouver le bon JAR (l'exécutable) et le renommer proprement
    find target/ -name "*.jar" -not -name "*original*" -exec cp {} target/app.jar \;

################################################################################

FROM package as extract
WORKDIR /build

# On pointe précisément sur le jar dans le sous-dossier
RUN java -Djarmode=layertools -jar dvdtheque-batch-service/target/app.jar extract --destination target/extracted

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

EXPOSE 8089

ENTRYPOINT [ "java",  "-Dspring.profiles.active=docker","-Dspring.cloud.config.label=develop","-agentlib:jdwp=transport=dt_socket,address=*:5009,server=y,suspend=n", "org.springframework.boot.loader.launch.JarLauncher"  ]
