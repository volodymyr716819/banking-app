# ---------- build stage ----------
    FROM eclipse-temurin:21-jdk AS builder
    WORKDIR /workspace
    
    COPY backend/mvnw mvnw
    COPY backend/.mvn .mvn
    COPY . .
    
    # make wrapper executable and build only the backend POM
    RUN chmod +x mvnw \
     && ./mvnw -f backend/pom.xml -ntp -DskipTests clean package
    
    
    # ---------- runtime stage ----------
    FROM eclipse-temurin:21-jre
    WORKDIR /app
    COPY --from=builder /workspace/backend/target/banking-backend-1.0.0.jar app.jar
    
    ENV PORT=8080
    EXPOSE 8080
    ENTRYPOINT ["java","-jar","/app/app.jar"]    