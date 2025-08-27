## Multi-stage build for Spring Boot (Java 21)
# Build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Leverage Gradle wrapper and build (layer-friendly)
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle build.gradle ./
COPY config ./config
RUN chmod +x ./gradlew

# Copy sources and build, then extract Spring Boot layers
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar \
  && java -Djarmode=layertools -jar build/libs/*.jar extract

# Runtime stage (JRE only)
FROM eclipse-temurin:21-jre-jammy AS runtime
LABEL org.opencontainers.image.source="https://github.com/Rujuu-prog/Tosk-back" \
      org.opencontainers.image.title="Tosk Backend" \
      org.opencontainers.image.description="Spring Boot backend for Tosk" \
      org.opencontainers.image.licenses="Proprietary"
ENV TZ=UTC \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -Duser.timezone=UTC"

WORKDIR /app
# install curl and tini for healthcheck and proper signal handling; create non-root user
RUN apt-get update -y \
  && apt-get install -y --no-install-recommends curl ca-certificates tini \
  && rm -rf /var/lib/apt/lists/* \
  && useradd -r -u 10001 -m -d /home/appuser -s /usr/sbin/nologin appuser

# Copy Spring Boot layers for better cache reuse
COPY --from=build /app/dependencies/ ./
COPY --from=build /app/snapshot-dependencies/ ./
COPY --from=build /app/spring-boot-loader/ ./
COPY --from=build /app/application/ ./

EXPOSE 8080
RUN chown -R appuser:appuser /app
USER appuser
HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["/usr/bin/tini","--","java","org.springframework.boot.loader.launch.JarLauncher"]
