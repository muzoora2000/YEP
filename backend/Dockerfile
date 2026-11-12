# Stage 1: build the executable Spring Boot jar with Maven.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

# Stage 2: small runtime image, non-root user, Render injects PORT.
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd --system yep && useradd --system --gid yep --home-dir /app yep
COPY --from=build /workspace/target/yep-backend-*.jar app.jar
RUN chown yep:yep app.jar
USER yep

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseSerialGC", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
