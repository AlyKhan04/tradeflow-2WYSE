# ============================================================================
# backend/Dockerfile — TICKET-I125 (Day 10)
# ============================================================================
# WHAT:    Multi-stage Dockerfile for the Spring Boot recon-service.
# HOW:     Stage 1 builds the JAR with Maven. Stage 2 runs it on a slim JRE.
# WHY:     Keeps Maven and the JDK out of the shipped image and isolates
#          build tooling from the final container.
# OBSERVE: `docker build .` succeeds; `docker run -p 8080:8080 <image>`
#          boots and `/actuator/health` returns UP.
# ============================================================================
# SIZE NOTE (TICKET-I125): the ticket targets < 250 MB; this image measures
# ~260 MB of layers (`docker images` reports ~411 MB — Docker 29's containerd
# store also counts attestation manifests). Breakdown:
#
#     135 MB  eclipse-temurin:17-jre-alpine (the JRE itself)
#      78 MB  Spring Boot fat JAR
#      46 MB  alpine rootfs + apk deps
#
# The multi-stage build IS working — `docker history` shows zero Maven layers
# in the runtime stage. The gap is the dependency set (web + jpa + security +
# kafka + liquibase + springdoc + micrometer), not the build. Getting under
# 250 MB needs a jlink'd custom runtime, which would mean dropping the
# `eclipse-temurin:17-jre-alpine` base the ticket also mandates. Keeping the
# mandated base was the deliberate choice.
# ============================================================================

# ---- Stage 1: build -------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy pom.xml FIRST and resolve dependencies in their own layer. Docker caches
# this layer keyed on pom.xml alone, so editing a .java file no longer forces a
# re-download of every artifact from Maven Central on the next build.
COPY pom.xml .
RUN mvn -B -e dependency:go-offline

# Source comes second — only this layer and the package step re-run on a code
# change.
COPY src ./src
RUN mvn -B -e clean package -DskipTests

# ---- Stage 2: runtime -----------------------------------------------------
# Only this stage ships. No Maven, no JDK, no source — just a JRE and the JAR.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8080

# Liveness probe — `docker ps` shows `(healthy)` once /actuator/health is UP.
# Alpine's busybox wget covers -qO-; no need to apk add the full wget.
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

# -XX:+UseContainerSupport lets the JVM read cgroup limits so heap sizing
# honours the container's memory cap (default on JDK 17, explicit is safer).
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
