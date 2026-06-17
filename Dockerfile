# Multi-stage build for ai-chat
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8095
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=90s \
  CMD curl -f http://localhost:8095/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
