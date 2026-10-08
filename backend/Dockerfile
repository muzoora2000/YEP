# Multi-stage build: compiles the Spring Boot fat jar, then runs it on a slim JRE.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Warm the dependency cache first so source-only changes rebuild fast.
COPY pom.xml .
RUN mvn -q -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd --system yep && useradd --system --gid yep yep
USER yep

COPY --from=build /workspace/target/yep-backend-*.jar app.jar

# Render injects PORT; the app reads it from the environment.
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
