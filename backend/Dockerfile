# syntax=docker/dockerfile:1

# --- Stage 0: build the React frontend (Step 3 of the homepage redesign) ---
FROM node:24-alpine AS frontend-build
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# --- Stage 1: build the executable jar ---
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

# Cache dependencies first (only re-runs when pom.xml changes)
COPY backend/pom.xml pom.xml
RUN mvn -q dependency:go-offline

# The pom requires a JDK-25 Maven toolchain. The base image's JDK lives at
# /opt/java/openjdk, so declare it as a toolchain for this build.
RUN mkdir -p /root/.m2 && printf '%s\n' \
  '<?xml version="1.0" encoding="UTF-8"?>' \
  '<toolchains>' \
  '  <toolchain>' \
  '    <type>jdk</type>' \
  '    <provides><version>25</version></provides>' \
  '    <configuration><jdkHome>/opt/java/openjdk</jdkHome></configuration>' \
  '  </toolchain>' \
  '</toolchains>' > /root/.m2/toolchains.xml

# Build the jar — the React build lands in static/app-next/ before Maven
# bundles the whole static/ tree onto the classpath, so it rides into the
# jar exactly like the hand-written static/index.html/app.js already do.
COPY backend/src src
COPY --from=frontend-build /frontend/dist src/main/resources/static/app-next
RUN mvn -q -DskipTests package

# --- Stage 2: slim runtime ---
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /build/target/firststep-backend-0.1.0.jar app.jar

# Bake runtime data at clean absolute paths, referenced via env below
COPY app/data/ /data/
COPY backend/src/main/resources/static/images/seasonal/ /seasonal/

ENV APP_DATA_DIR=/data
ENV APP_SEASONAL_IMAGES_DIR=/seasonal

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
