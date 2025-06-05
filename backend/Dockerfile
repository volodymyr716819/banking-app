# ---------- build stage ----------
    FROM eclipse-temurin:21-jdk AS builder
    WORKDIR /workspace
    
    # ── copy the Maven wrapper explicitly ────────────────────────────────────────
    COPY mvnw mvnw
    COPY .mvn .mvn
    # ── copy the rest of the source tree ────────────────────────────────────────
    COPY . .
    
    # build only the backend module, skip tests for speed
    RUN ./mvnw -ntp -DskipTests -pl backend -am clean package
    
    
    # ---------- runtime stage ----------
    FROM eclipse-temurin:21-jre
    WORKDIR /app
    COPY --from=builder /workspace/backend/target/banking-backend-1.0.0.jar app.jar
    
    ENV PORT=8080               
    EXPOSE 8080
    ENTRYPOINT ["java","-jar","/app/app.jar"]
    
    