FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 spring
COPY --from=builder /workspace/build/libs/*.jar /app/app.jar
USER spring
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
