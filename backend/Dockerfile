# ---------- build stage ----------
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

COPY mvnw mvnw
COPY .mvn .mvn
COPY . .

RUN chmod +x mvnw \
 && ./mvnw -ntp -DskipTests clean package
