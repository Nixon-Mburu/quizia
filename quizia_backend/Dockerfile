# Stage 1: build with Gradle + JDK21
FROM gradle:8-jdk21 AS builder
WORKDIR /workspace

# Copy the backend application
COPY . /workspace/

# Build the application (skip tests)
RUN gradle clean build -x test --no-daemon

# Stage 2: runtime with a lightweight JRE
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /workspace/build/libs/quizia_backend-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
