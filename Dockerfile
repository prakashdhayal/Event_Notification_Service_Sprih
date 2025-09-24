# Use official OpenJDK 17 runtime as base image
FROM openjdk:17-jdk-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Set working directory inside container
WORKDIR /app

# Copy Maven wrapper and pom.xml first for better Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Make Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src/ ./src/

# Build the application
RUN ./mvnw clean package -DskipTests

# Create a non-root user for security
RUN useradd --create-home --shell /bin/bash app

# Create directory for application and change ownership
RUN mkdir -p /app/runtime && chown -R app:app /app

# Switch to non-root user
USER app

# Copy the built JAR file to runtime directory
RUN cp target/*.jar /app/runtime/app.jar

# Set working directory to runtime
WORKDIR /app/runtime

# Expose port 8080
EXPOSE 8080

# Set JVM options for container environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Health check to ensure application is running
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/webhook/callback || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]