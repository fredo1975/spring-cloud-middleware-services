# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk as deps
WORKDIR /build

# 1. Préparation des Commons (Installation dans le repo local)
COPY dvdtheque-commons/ dvdtheque-commons/
COPY dvdtheque-service/mvnw dvdtheque-service/mvnw
COPY dvdtheque-service/.mvn/ dvdtheque-service/.mvn/

RUN chmod +x dvdtheque-service/mvnw && \
    cd dvdtheque-commons && \
    ../dvdtheque-service/mvnw install -DskipTests

# 2. Préparation du Service (Download des dépendances)
COPY dvdtheque-service/pom.xml dvdtheque-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    cd dvdtheque-service && ./mvnw dependency:go-offline -DskipTests

################################################################################

FROM deps as package
WORKDIR /build

# On copie les sources du service
COPY dvdtheque-service/src dvdtheque-service/src

# COMPILATION ET RENOMMAGE SÉCURISÉ
RUN --mount=type=cache,target=/root/.m2 \
    cd dvdtheque-service && \
    ./mvnw package -DskipTests && \
    # Utilisation de find pour trouver le bon JAR (l'exécutable) et le renommer proprement
    find target/ -name "*.jar" -not -name "*original*" -exec cp {} target/app.jar \;

################################################################################

FROM package as extract
WORKDIR /build

# On pointe précisément sur le jar dans le sous-dossier
RUN java -Djarmode=layertools -jar dvdtheque-service/target/app.jar extract --destination target/extracted

################################################################################

FROM eclipse-temurin:21-jdk AS final
WORKDIR /app

ARG UID=10001
RUN adduser --disabled-password --gecos "" --home "/nonexistent" --shell "/sbin/nologin" --no-create-home --uid "${UID}" appuser
USER appuser

# On récupère les couches depuis le dossier 'target/extracted' de l'étape précédente
COPY --from=extract /build/target/extracted/dependencies/ ./
COPY --from=extract /build/target/extracted/spring-boot-loader/ ./
COPY --from=extract /build/target/extracted/snapshot-dependencies/ ./
COPY --from=extract /build/target/extracted/application/ ./

EXPOSE 8083

ENTRYPOINT [ "java", "-Dspring.profiles.active=docker","-Dspring.cloud.config.label=develop","-agentlib:jdwp=transport=dt_socket,address=*:5005,server=y,suspend=n","org.springframework.boot.loader.launch.JarLauncher" ]