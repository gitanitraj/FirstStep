# syntax=docker/dockerfile:1

# --- Stage 1: build the executable jar ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache dependencies first (only re-runs when pom.xml changes)
COPY backend/pom.xml pom.xml
RUN mvn -q dependency:go-offline

# The pom requires a JDK-17 Maven toolchain. The base image's JDK lives at
# /opt/java/openjdk, so declare it as a toolchain for this build.
RUN mkdir -p /root/.m2 && printf '%s\n' \
  '<?xml version="1.0" encoding="UTF-8"?>' \
  '<toolchains>' \
  '  <toolchain>' \
  '    <type>jdk</type>' \
  '    <provides><version>17</version></provides>' \
  '    <configuration><jdkHome>/opt/java/openjdk</jdkHome></configuration>' \
  '  </toolchain>' \
  '</toolchains>' > /root/.m2/toolchains.xml

# Build the jar
COPY backend/src src
RUN mvn -q -DskipTests package

# --- Stage 2: slim runtime ---
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /build/target/firststep-backend-0.1.0.jar app.jar

# Bake runtime data at clean absolute paths, referenced via env below
COPY app/data/ /data/
COPY backend/src/main/resources/static/images/seasonal/ /seasonal/

ENV APP_DATA_DIR=/data
ENV APP_SEASONAL_IMAGES_DIR=/seasonal

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
