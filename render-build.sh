#!/usr/bin/env bash
# Build the backend JAR without running tests ( Render's free tier is slow ).
./backend/mvnw -ntp -DskipTests clean package