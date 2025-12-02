# Stage 1: Build stage with Gradle and JDK 21
FROM gradle:8-jdk21 AS builder

WORKDIR /app


COPY . .

RUN cd quizia_backend && gradle clean build -x test --no-daemon


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app


COPY --from=builder /app/quizia_backend/build/libs/quizia_backend-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
