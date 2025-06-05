# ---- build stage ----
    FROM eclipse-temurin:21-jdk AS builder
    WORKDIR /workspace
    COPY . .
    RUN ./mvnw -ntp -DskipTests clean package
    
    # ---- run stage ----
    FROM eclipse-temurin:21-jre
    WORKDIR /app
    COPY --from=builder /workspace/target/banking-backend-1.0.0.jar app.jar
    ENV PORT=8080               
    EXPOSE 8080
    ENTRYPOINT ["java","-jar","/app/app.jar"]
    