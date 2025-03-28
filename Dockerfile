# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Define build argument for the module to build
ARG MODULE_NAME

# Copy the entire project
COPY . .

# Download all required dependencies into one layer
RUN mvn dependency:go-offline -B -pl ${MODULE_NAME}

# Build the application
RUN mvn clean package -pl ${MODULE_NAME} -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Define build argument for the module to build (needed for COPY)
ARG MODULE_NAME

# Copy the built artifact from the build stage
COPY --from=build /app/${MODULE_NAME}/target/*.jar app.jar

# Set the entrypoint to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Expose the application port (will be overridden by docker-compose)
EXPOSE 8080