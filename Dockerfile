# Shared multi-stage build for all service modules.
# Usage: docker build --build-arg MODULE=member-service .
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace
COPY . .
ARG MODULE
# gradlew may be checked out with CRLF on Windows hosts.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
    && ./gradlew :${MODULE}:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
ARG MODULE
COPY --from=builder /workspace/${MODULE}/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
