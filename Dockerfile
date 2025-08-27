## Multi-stage build for Spring Boot (Java 21)
# Build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Leverage Gradle wrapper
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle build.gradle ./
COPY config ./config
COPY src ./src

RUN chmod +x ./gradlew \
  && ./gradlew --no-daemon clean bootJar

# Runtime stage (JRE only)
FROM eclipse-temurin:21-jre-jammy AS runtime
ENV TZ=UTC \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8 -Duser.timezone=UTC"

WORKDIR /app
# install curl for container healthcheck
RUN apt-get update -y \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
