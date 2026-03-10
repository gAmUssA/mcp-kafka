# Stage 1: Build
FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21 AS build
COPY --chown=quarkus:quarkus . /project
WORKDIR /project
RUN ./gradlew build -Dquarkus.native.enabled=true -x test

# Stage 2: Runtime
FROM quay.io/quarkus/quarkus-micro-image:2.0
COPY --from=build /project/build/*-runner /application
EXPOSE 8080
CMD ["./application"]
