# Generic multi-stage image for any Java service in backend/java-services.
# Build context: backend/java-services
#   docker build -f infrastructure/docker/java-service.Dockerfile --build-arg SERVICE=identity-service backend/java-services

FROM maven:3.9-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /src
COPY pom.xml .
COPY platform-common/pom.xml platform-common/pom.xml
COPY ${SERVICE}/pom.xml ${SERVICE}/pom.xml
COPY platform-common/src platform-common/src
COPY ${SERVICE}/src ${SERVICE}/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl ${SERVICE} -am package -DskipTests \
    && cp ${SERVICE}/target/${SERVICE}-*.jar /src/app.jar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S nexus && adduser -S nexus -G nexus
WORKDIR /app
COPY --from=build /src/app.jar /app/app.jar
USER nexus
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
