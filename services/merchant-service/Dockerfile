# =============================================================
# merchant-service -- Dockerfile
# Multi-stage: Maven build -> minimal JRE runtime -- Java 21
# =============================================================

# ---- Stage 1: Build ----
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q

COPY src/ src/
RUN ./mvnw package -Dmaven.test.skip=true -q

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

WORKDIR /app

COPY --from=builder /build/target/merchant-service-*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8084

ENTRYPOINT ["java",   "-XX:+UseContainerSupport",   "-XX:MaxRAMPercentage=75.0",   "-Djava.security.egd=file:/dev/./urandom",   "-jar", "app.jar"]
