# IDTrust E15 - production image for the app module. Multi-stage: build with the Gradle wrapper
# (must be generated and committed first, see root README's "Prerequisites"/"Running locally"),
# run on a minimal JRE. Build from the `backend/` directory: `docker build -t idtrust/backend .`
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew :app:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
# Runs as a non-root user (Master Spec section 33's security controls) rather than the default
# root user every plain JRE base image starts containers as.
RUN useradd --system --uid 1000 idtrust
COPY --from=build /workspace/app/build/libs/idtrust-app.jar app.jar
USER idtrust
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
