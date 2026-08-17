# Stage 1: Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradlew.bat .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy source code
COPY src src

# Grant execute permission to gradlew and build the application
RUN chmod +x gradlew && ./gradlew build -x test

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the JAR file from the builder stage
COPY --from=builder /app/build/libs/demo-1.0.0.jar app.jar

# Expose the port
EXPOSE 8080

# Set environment variables for production profile
ENV SPRING_PROFILES_ACTIVE=prod

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
