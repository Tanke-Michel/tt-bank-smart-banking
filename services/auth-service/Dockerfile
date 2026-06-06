# =============================================================
# Auth Service — Dockerfile
# Multi-stage build: Maven build → minimal JRE runtime image
# Java 21 (matches pom.xml <java.version>21</java.version>)
# =============================================================

# ---- Stage 1: Build ----
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy Maven wrapper and POM first to cache dependency layer
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -q

# Copy source code and build
COPY src/ src/
RUN ./mvnw package -Dmaven.test.skip=true -q

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-jammy

# Non-root user for security
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

WORKDIR /app

# Copy the fat JAR from the build stage
COPY --from=builder /build/target/auth-service-*.jar app.jar

# Give ownership to non-root user
RUN chown appuser:appgroup app.jar

USER appuser

# Expose service port
EXPOSE 8081

# JVM tuning: container-aware, enable virtual threads (Java 21)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
