# ---------- build stage ----------
    FROM eclipse-temurin:21-jdk AS builder
    WORKDIR /workspace
    
    COPY backend/mvnw mvnw
    COPY backend/.mvn .mvn
    COPY . .
    
    # give the wrapper execute permission, then build
    RUN chmod +x mvnw \
     && ./mvnw -ntp -DskipTests -pl backend -am clean package      
    
    # ---------- runtime stage ----------
    FROM eclipse-temurin:21-jre
    WORKDIR /app
    
    # copy the built JAR from the builder stage
    COPY --from=builder /workspace/backend/target/banking-backend-1.0.0.jar app.jar
    
    ENV PORT=8080               
    EXPOSE 8080
    ENTRYPOINT ["java","-jar","/app/app.jar"]    