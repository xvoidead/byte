# Сборка фронтенда
FROM node:22-alpine AS frontend
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Сборка бэкенда вместе со статикой фронтенда
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY backend/pom.xml ./
RUN mvn -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /app/dist ./src/main/resources/static
RUN mvn -q -DskipTests package

# Запуск. Нужен именно JDK: сервер компилирует код встроенным javac.
FROM eclipse-temurin:21-jdk
RUN useradd --system --uid 10001 --home-dir /app byte
WORKDIR /app
COPY --from=backend /app/target/byte.jar app.jar
USER byte
ENV JAVA_OPTS="-Xmx512m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
