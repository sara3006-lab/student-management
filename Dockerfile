# ===== STAGE 1 : Build Frontend React =====
FROM node:20-alpine AS frontend-build
WORKDIR /frontend
COPY student-frontend-react/package*.json ./
RUN npm install
COPY student-frontend-react/ .
RUN npm run build

# ===== STAGE 2 : Build Backend Spring Boot =====
FROM maven:3.9.6-eclipse-temurin-21 AS backend-build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# ===== STAGE 3 : Image finale =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copier le JAR backend
COPY --from=backend-build /app/target/student-management-0.0.1-SNAPSHOT.jar app.jar

# Copier le build frontend dans resources/static
COPY --from=frontend-build /frontend/build /app/static

EXPOSE 8089

ENTRYPOINT ["java", "-jar", "app.jar"]
