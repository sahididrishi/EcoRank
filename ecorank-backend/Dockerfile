FROM node:18-alpine AS frontend-build
WORKDIR /frontend
COPY ecorank-frontend/package.json ecorank-frontend/package-lock.json ./
RUN npm ci
COPY ecorank-frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /app
COPY ecorank-backend/pom.xml .
RUN mvn dependency:go-offline -B
COPY ecorank-backend/src ./src
COPY --from=frontend-build /frontend/dist ./src/main/resources/static/
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S ecorank && adduser -S ecorank -G ecorank
COPY --from=backend-build /app/target/ecorank-backend-*.jar app.jar
USER ecorank
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
