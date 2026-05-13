# Build stage
FROM eclipse-temurin:23-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

# Run stage
FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Kích hoạt application-prod.yml (ddl-auto: validate, không show SQL...)
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
